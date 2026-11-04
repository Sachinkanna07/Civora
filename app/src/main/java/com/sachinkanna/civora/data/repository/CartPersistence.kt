package com.sachinkanna.civora.data.repository

import android.content.Context
import com.sachinkanna.civora.data.model.*
import org.json.JSONArray
import org.json.JSONObject

data class PersistedCart(val requestId: String = java.util.UUID.randomUUID().toString(), val items: List<CartItem> = emptyList())
interface CartPersistence {
    fun load(uid: String): PersistedCart
    fun save(uid: String, cart: PersistedCart)
    fun clear(uid: String)
}
class MemoryCartPersistence : CartPersistence {
    private val carts = mutableMapOf<String, PersistedCart>()
    override fun load(uid: String) = carts[uid] ?: PersistedCart()
    override fun save(uid: String, cart: PersistedCart) { carts[uid] = cart }
    override fun clear(uid: String) { carts.remove(uid) }
}
class DeviceCartPersistence(context: Context) : CartPersistence {
    private val prefs = context.applicationContext.getSharedPreferences("civora_private_cart", Context.MODE_PRIVATE)
    override fun load(uid: String): PersistedCart = runCatching {
        val value = prefs.getString(uid, null) ?: return PersistedCart()
        val obj = JSONObject(value)
        val list = obj.getJSONArray("items")
        PersistedCart(obj.getString("requestId"), (0 until list.length()).map { index ->
            val i = list.getJSONObject(index)
            CartItem(MenuItem(i.getString("id"), i.getString("canteenId"), i.getString("name"), i.getLong("pricePaise"), i.optBoolean("available",true)),i.getInt("quantity"))
        }.filter { it.quantity in 1..20 }.take(30))
    }.getOrElse { PersistedCart() }
    override fun save(uid: String, cart: PersistedCart) {
        if(uid.isBlank()) return
        val items = JSONArray()
        cart.items.forEach { items.put(JSONObject().put("id",it.item.id).put("canteenId",it.item.canteenId).put("name",it.item.name).put("pricePaise",it.item.pricePaise).put("available",it.item.available).put("quantity",it.quantity)) }
        prefs.edit().putString(uid, JSONObject().put("requestId",cart.requestId).put("items",items).toString()).apply()
    }
    override fun clear(uid: String) { prefs.edit().remove(uid).apply() }
}
