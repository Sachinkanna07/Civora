package com.sachinkanna.civora.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sachinkanna.civora.data.model.Announcement
import com.sachinkanna.civora.data.model.CampusEvent
import com.sachinkanna.civora.data.repository.AnnouncementRepository
import com.sachinkanna.civora.data.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnnouncementUiState(val loading:Boolean=true, val items:List<Announcement> = emptyList(), val category:String?=null, val departmentOnly:Boolean=false, val error:String?=null) {
    val filtered get() = items.filter { (category == null || it.category.equals(category, true)) && (!departmentOnly || it.department.isNotBlank()) }
}
class AnnouncementViewModel(private val repo: AnnouncementRepository = AnnouncementRepository()): ViewModel() {
    private val _state=MutableStateFlow(AnnouncementUiState()); val state:StateFlow<AnnouncementUiState> = _state.asStateFlow()
    init { refresh() }
    fun refresh() { viewModelScope.launch { _state.value=_state.value.copy(loading=true,error=null); runCatching { repo.getFeed() }.onSuccess { _state.value=_state.value.copy(loading=false,items=it) }.onFailure { _state.value=_state.value.copy(loading=false,error="Announcements are unavailable right now.") } } }
    fun setCategory(value:String?) { _state.value=_state.value.copy(category=value) }
    fun setDepartmentOnly(value:Boolean) { _state.value=_state.value.copy(departmentOnly=value) }
    fun loadDetail(id:String) { viewModelScope.launch { runCatching { repo.get(id) }.onSuccess { item -> _state.value=_state.value.copy(items=listOf(item),loading=false) } } }
}

data class EventsUiState(val loading:Boolean=true, val items:List<CampusEvent> = emptyList(), val selectedCategory:String?=null, val upcomingOnly:Boolean=true, val registeredIds:Set<String> = emptySet(), val error:String?=null, val actionInProgress:String?=null) {
    val filtered get() = items.filter { (selectedCategory == null || it.category.equals(selectedCategory,true)) && (!upcomingOnly || it.startTime == 0L || it.startTime >= System.currentTimeMillis()) }
}
class EventsViewModel(private val repo: EventsRepository = EventsRepository()): ViewModel() {
    private val _state=MutableStateFlow(EventsUiState()); val state:StateFlow<EventsUiState> = _state.asStateFlow()
    fun load(uid:String) { viewModelScope.launch { _state.value=_state.value.copy(loading=true,error=null); runCatching { repo.getUpcoming() to repo.registeredEventIds(uid) }.onSuccess { (events,ids)->_state.value=_state.value.copy(loading=false,items=events,registeredIds=ids) }.onFailure { _state.value=_state.value.copy(loading=false,error="Events are unavailable right now.") } } }
    fun setCategory(value:String?) { _state.value=_state.value.copy(selectedCategory=value) }
    fun setUpcomingOnly(value:Boolean) { _state.value=_state.value.copy(upcomingOnly=value) }
    fun toggleRegistration(eventId:String, uid:String) { if (_state.value.actionInProgress != null) return; viewModelScope.launch { val registered=eventId in _state.value.registeredIds; _state.value=_state.value.copy(actionInProgress=eventId,error=null); runCatching { if (registered) repo.cancel(eventId,uid) else repo.register(eventId,uid) }.onSuccess { val ids=_state.value.registeredIds.toMutableSet().apply { if (registered) remove(eventId) else add(eventId) }; _state.value=_state.value.copy(registeredIds=ids,actionInProgress=null) }.onFailure { _state.value=_state.value.copy(actionInProgress=null,error="Could not update your RSVP.") } } }
    fun loadDetail(id:String,uid:String) { viewModelScope.launch { runCatching { repo.get(id) to repo.registeredEventIds(uid) }.onSuccess { (event,ids)->_state.value=_state.value.copy(items=listOf(event),registeredIds=ids,loading=false) } } }
}
