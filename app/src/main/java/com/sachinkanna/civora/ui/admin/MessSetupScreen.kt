package com.sachinkanna.civora.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun MessSetupScreen(state: WorkspaceState,vm: CampusWorkspaceViewModel,back: ()->Unit) {
    var mode by rememberSaveable {mutableStateOf("Hall")};var id by rememberSaveable {mutableStateOf(java.util.UUID.randomUUID().toString())}
    var hallId by rememberSaveable {mutableStateOf("")};var name by rememberSaveable {mutableStateOf("")};var operatorId by rememberSaveable {mutableStateOf("")};var open by rememberSaveable {mutableStateOf(true)}
    var label by rememberSaveable {mutableStateOf("Lunch")};var starts by rememberSaveable {mutableLongStateOf(0)};var ends by rememberSaveable {mutableLongStateOf(0)}
    var studentId by rememberSaveable {mutableStateOf("")};var active by rememberSaveable {mutableStateOf(true)}
    val halls=state.values<MessHall>(CampusFeed.MESS_HALLS)
    CampusPage("Mess configuration",back) {
        item {FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Hall","Meal period","Entitlement").forEach {m->FilterChip(mode==m,{mode=m;id=java.util.UUID.randomUUID().toString()},label={Text(m)})}}}
        if(mode=="Hall") {
            item {SelectField("Existing hall",if(halls.any {it.id==id})id else "",halls.map {Choice(it.id,it.name)},{chosen->halls.find {it.id==chosen}?.let {id=it.id;name=it.name;operatorId=it.operatorId;open=it.open}},allowNone=true)}
            item {OutlinedTextField(name,{name=it.take(100)},label={Text("Mess hall name")},modifier=Modifier.fillMaxWidth())}
            item {SelectField("Mess operator",operatorId,state.values<StaffMember>(CampusFeed.STAFF).filter {it.role=="mess"&&it.active}.map {Choice(it.uid,it.name)},{operatorId=it})}
            item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text("Open for meal check-in");Switch(open,{open=it})}}
            item {Button(onClick={vm.action("manageMess",mapOf("kind" to "hall","id" to id,"name" to name,"operatorId" to operatorId,"open" to open),"Mess hall saved")},enabled=name.isNotBlank()&&operatorId.isNotBlank()&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Save mess hall")}}
        } else {
            item {SelectField("Mess hall",hallId,halls.map {Choice(it.id,it.name)},{hallId=it})}
            if(mode=="Meal period") {
                val meals=state.values<MealPeriod>(CampusFeed.MEAL_PERIODS).filter {it.messId==hallId}
                item {SelectField("Existing meal period",if(meals.any {it.id==id})id else "",meals.map {Choice(it.id,it.label,dateTime(it.opensAt))},{chosen->meals.find {it.id==chosen}?.let {id=it.id;label=it.label;starts=it.opensAt;ends=it.closesAt}},allowNone=true)}
                item {OutlinedTextField(label,{label=it.take(80)},label={Text("Meal label")},modifier=Modifier.fillMaxWidth())}
                item {DateTimeField("Check-in opens",starts,{starts=it});DateTimeField("Check-in closes",ends,{ends=it})}
                item {Button(onClick={vm.action("manageMess",mapOf("kind" to "period","id" to id,"messId" to hallId,"label" to label,"opensAt" to starts,"closesAt" to ends),"Meal period saved")},enabled=!state.busy&&hallId.isNotBlank()&&label.isNotBlank()&&starts>0&&ends>starts&&ends-starts<=12*3600000,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Save meal period")}}
            } else {
                item {SelectField("Student",studentId,state.values<StaffMember>(CampusFeed.STUDENTS).map {Choice(it.uid,it.name,it.department)},{studentId=it})}
                item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text("Meal entitlement active");Switch(active,{active=it})}}
                item {Button(onClick={vm.action("manageMess",mapOf("kind" to "entitlement","messId" to hallId,"userId" to studentId,"active" to active),"Meal entitlement updated")},enabled=!state.busy&&hallId.isNotBlank()&&studentId.isNotBlank(),modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Save entitlement")}}
            }
        }
        item {TextButton(onClick={id=java.util.UUID.randomUUID().toString();name="";label="Lunch";starts=0;ends=0}) {Text("Start new record")}}
    }
}
