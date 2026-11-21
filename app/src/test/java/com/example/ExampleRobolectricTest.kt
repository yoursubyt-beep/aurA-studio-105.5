package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AspectRatioOption
import com.example.data.BodyArchetype
import com.example.data.PromptTools
import com.example.data.StudioCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aura Studio", appName)
    }

    @Test
    fun `prompt refiner replaces sensitive phrasing cleanly`() {
        val refined = PromptTools.refinePromptEditorial("transparent fabric with natural fats")
        assertTrue(refined.contains("translucent gossamer sheer fabric"))
        assertTrue(refined.contains("natural soft anatomical fullness"))
    }

    @Test
    fun `compiled prompt includes archetype, costume, and focus modifiers`() {
        val compiled = PromptTools.compileStudioPrompt(
            intent = "editorial portrait",
            autoRefine = true,
            outfitPrompt = StudioCatalog.builtinCostumes.first().prompt,
            underlayer = StudioCatalog.underlayerOptions.first(),
            sheerness = StudioCatalog.sheernessOptions.first(),
            navelStyle = StudioCatalog.navelOptions.first(),
            framing = StudioCatalog.framingOptions.first(),
            identityNotes = "warm olive skin",
            archetype = BodyArchetype.SOFT_CURVE,
            bustCm = 92,
            waistCm = 66,
            hipsCm = 96,
            limbSoftness = 75,
            skinSheen = 60,
            activeChips = StudioCatalog.defaultChips,
            references = emptyList(),
            cameraYaw = 30,
            radarX = 0.2f,
            radarY = -0.3f
        )
        assertTrue(compiled.contains("Soft Curve"))
        assertTrue(compiled.contains("36D"))
        assertTrue(compiled.contains("Camera orbit 30 degrees"))
        assertEquals(AspectRatioOption.PORTRAIT_3_4.ratioText, "3:4")
    }
}
