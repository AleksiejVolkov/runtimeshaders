package com.offmind.runtimeshaders.screens

import androidx.compose.runtime.Composable
import com.offmind.runtimeshaders.navigation.Route

internal sealed interface EffectCatalogItem {
    val id: String
    val title: String
    val description: String
    val effectCount: Int
}

internal data class EffectCategory(
    override val id: String,
    override val title: String,
    override val description: String,
    val children: List<EffectCatalogItem>,
) : EffectCatalogItem {
    override val effectCount: Int get() = children.sumOf { it.effectCount }
}

internal data class EffectScreenData(
    override val id: String,
    override val title: String,
    override val description: String,
    val content: @Composable (onNavigate: (Route) -> Unit) -> Unit,
) : EffectCatalogItem {
    override val effectCount: Int get() = 1
}

/**
 * One tree owns the menu, screen registration and breadcrumbs. IDs are stable navigation keys:
 * keep them when renaming or moving entries so restored back stacks still resolve their screens.
 */
internal class EffectCatalog(val root: EffectCategory) {
    private val itemsById = mutableMapOf<String, EffectCatalogItem>()
    private val ancestorsById = mutableMapOf<String, List<EffectCategory>>()

    init {
        fun register(item: EffectCatalogItem, ancestors: List<EffectCategory>) {
            require(item.id.isNotBlank()) { "Catalog IDs must not be blank" }
            require(item.title.isNotBlank()) { "Catalog title must not be blank: ${item.id}" }
            require(itemsById.put(item.id, item) == null) { "Duplicate catalog ID: ${item.id}" }
            ancestorsById[item.id] = ancestors
            if (item is EffectCategory) {
                require(item.children.isNotEmpty()) { "Empty catalog category: ${item.id}" }
                item.children.forEach { register(it, ancestors + item) }
            }
        }
        register(root, emptyList())
    }

    fun category(id: String): EffectCategory =
        requireNotNull(itemsById[id] as? EffectCategory) { "Unknown catalog category: $id" }

    fun effect(id: String): EffectScreenData =
        requireNotNull(itemsById[id] as? EffectScreenData) { "Unknown catalog effect: $id" }

    fun ancestors(id: String): List<EffectCategory> =
        requireNotNull(ancestorsById[id]) { "Unknown catalog item: $id" }
}
