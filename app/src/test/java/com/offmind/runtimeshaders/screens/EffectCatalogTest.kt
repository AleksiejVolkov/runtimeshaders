package com.offmind.runtimeshaders.screens

import com.offmind.runtimeshaders.navigation.Route
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class EffectCatalogTest {
    @Test
    fun `all existing demos remain reachable exactly once from the root`() {
        fun collectEffects(item: EffectCatalogItem): List<String> = when (item) {
            is EffectCategory -> item.children.flatMap(::collectEffects)
            is EffectScreenData -> listOf(item.id)
        }
        val ids = collectEffects(effectsCatalog.root)
        val expectedIds = setOf(
            "glass-basics", "glass-set", "neumorphism-basics", "neumorphism-accents",
            "neumorphism-thermostat", "dual-light-texture", "illuminate-ui", "colorful-toggle",
            "neon-fog-button", "circular-timer", "metal-knob", "opengl-glass", "tape-plane", "waveshock",
            "canvas-deform", "metaballs", "lamp-shadow", "snowed-dialog", "burn-after-reading",
            "navigation-test", "engine-glow-buttons", "engine-deferred-reads", "engine-shader-area",
            "engine-shader-cost", "engine-vs-metaballs", "engine-vs-shadows", "engine-vs-distortion",
            "engine-vs-dust", "engine-vs-edgefade", "engine-vs-normalmap",
            "engine-vs-vinyl", "engine-vs-charge", "engine-vs-toggle",
            "engine-vs-knob", "engine-vs-progblur", "engine-hero-nav",
            "engine-hero-bulb",
        )
        assertEquals(expectedIds, ids.toSet())
        assertEquals(expectedIds.size, ids.size)
        assertEquals(ids.size, effectsCatalog.root.effectCount)
        ids.forEach { assertEquals(it, effectsCatalog.effect(it).id) }
    }

    @Test
    fun `widgets contain separate glass neumorphism lighting and controls collections`() {
        assertEquals(
            listOf("glass", "neumorphism", "lighting", "controls"),
            effectsCatalog.category("widgets").children.map { it.id },
        )
        assertEquals(
            listOf("dual-light-texture", "illuminate-ui"),
            effectsCatalog.category("lighting").children.map { it.id },
        )
        assertEquals(
            listOf("opengl-glass", "tape-plane"),
            effectsCatalog.category("opengl-scenes").children.map { it.id },
        )
        assertEquals(11, effectsCatalog.category("widgets").effectCount)
    }

    @Test
    fun `compose engine demos form one group at the root in their order`() {
        assertEquals(
            listOf(
                "engine-hero-bulb", "engine-hero-nav", "engine-glow-buttons", "engine-deferred-reads", "engine-shader-area", "engine-shader-cost",
                "engine-vs-metaballs", "engine-vs-shadows", "engine-vs-distortion", "engine-vs-dust",
                "engine-vs-edgefade", "engine-vs-normalmap", "engine-vs-vinyl",
                "engine-vs-charge", "engine-vs-toggle", "engine-vs-knob",
                "engine-vs-progblur",
            ),
            effectsCatalog.category("compose-engine").children.map { it.id },
        )
        assertEquals("compose-engine", effectsCatalog.ancestors("engine-shader-cost").last().id)
        assertEquals("effects", effectsCatalog.ancestors("compose-engine").single().id)
    }

    @Test
    fun `arbitrarily nested categories resolve their complete breadcrumb path`() {
        val demo = effect("demo")
        val deep = category("deep", demo)
        val section = category("section", deep)
        val root = category("root", section)
        val catalog = EffectCatalog(root)

        assertSame(demo, catalog.effect("demo"))
        assertSame(deep, catalog.category("deep"))
        assertEquals(listOf(root, section, deep), catalog.ancestors("demo"))
        assertEquals(listOf(root, section), catalog.ancestors("deep"))
        assertEquals(emptyList<EffectCategory>(), catalog.ancestors("root"))
        assertEquals(1, root.effectCount)
    }

    @Test
    fun `saved route IDs still resolve when a demo is renamed and moved`() {
        val savedEffect = Json.encodeToString(Route.Effect.serializer(), Route.Effect("demo"))
        val savedCategory = Json.encodeToString(Route.Category.serializer(), Route.Category("section"))
        val movedDemo = effect("demo").copy(title = "Renamed demo")
        val catalog = EffectCatalog(category("root", category("section", category("new", movedDemo))))

        val restoredEffect = Json.decodeFromString(Route.Effect.serializer(), savedEffect)
        val restoredCategory = Json.decodeFromString(Route.Category.serializer(), savedCategory)
        assertSame(movedDemo, catalog.effect(restoredEffect.effectId))
        assertEquals("section", catalog.category(restoredCategory.categoryId).id)
        assertEquals(Route.Effect("demo"), restoredEffect)
        assertEquals(Route.Category("section"), restoredCategory)
    }

    @Test
    fun `duplicate IDs across branches or item types fail at registration`() {
        assertThrows(IllegalArgumentException::class.java) {
            EffectCatalog(category("root", category("a", effect("same")), category("b", effect("same"))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            EffectCatalog(category("root", category("same", effect("demo")), effect("same")))
        }
    }

    @Test
    fun `empty categories and blank IDs fail at registration`() {
        assertThrows(IllegalArgumentException::class.java) { EffectCatalog(category("root")) }
        assertThrows(IllegalArgumentException::class.java) { EffectCatalog(category("root", effect(" "))) }
    }

    @Test
    fun `lookup rejects a missing ID or the wrong destination type`() {
        val catalog = EffectCatalog(category("root", effect("demo")))
        assertThrows(IllegalArgumentException::class.java) { catalog.effect("missing") }
        assertThrows(IllegalArgumentException::class.java) { catalog.effect("root") }
        assertThrows(IllegalArgumentException::class.java) { catalog.category("demo") }
    }

    private fun effect(id: String) = EffectScreenData(id, "Demo", "Description") { }

    private fun category(id: String, vararg children: EffectCatalogItem) =
        EffectCategory(id, id, "Description", children.toList())
}
