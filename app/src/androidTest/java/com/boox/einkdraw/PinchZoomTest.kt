package com.boox.einkdraw

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinchZoomTest {

    private val TIMEOUT = 5000L

    @Test
    fun testPinchZoomRapidOff() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Ensure Rapid mode is OFF
        val rapidButton = device.wait(Until.findObject(By.res("com.boox.einkdraw:id/buttonRapidMode")), TIMEOUT)
        assertNotNull("Rapid button should be visible", rapidButton)
        if (rapidButton.text.contains("ON", ignoreCase = true)) {
            rapidButton.click()
            device.wait(Until.findObject(By.textContains("OFF")), TIMEOUT)
        }

        val uiDrawingView = device.findObject(By.res("com.boox.einkdraw:id/drawingView"))
        assertNotNull("DrawingView should be visible", uiDrawingView)

        var initialZoom = 1f
        scenario.onActivity { activity ->
            val drawingView = activity.findViewById<DrawingView>(R.id.drawingView)
            initialZoom = drawingView.getViewport().first
        }
        
        // Perform pinch open (zoom in)
        uiDrawingView.pinchOpen(0.8f)
        
        // Give some time for the gesture and UI update
        Thread.sleep(1000)

        scenario.onActivity { activity ->
            val drawingView = activity.findViewById<DrawingView>(R.id.drawingView)
            val newZoom = drawingView.getViewport().first
            assertTrue("Zoom should have increased when Rapid is OFF. Initial: $initialZoom, New: $newZoom", newZoom > initialZoom)
        }
        scenario.close()
    }

    @Test
    fun testPinchZoomRapidOn() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Ensure Rapid mode is ON
        val rapidButton = device.wait(Until.findObject(By.res("com.boox.einkdraw:id/buttonRapidMode")), TIMEOUT)
        assertNotNull("Rapid button should be visible", rapidButton)
        if (rapidButton.text.contains("OFF", ignoreCase = true)) {
            rapidButton.click()
            // Wait for service to start and state to sync
            device.wait(Until.findObject(By.textContains("ON")), TIMEOUT)
        }

        val uiDrawingView = device.findObject(By.res("com.boox.einkdraw:id/drawingView"))
        assertNotNull("DrawingView should be visible", uiDrawingView)

        var initialZoom = 1f
        scenario.onActivity { activity ->
            val drawingView = activity.findViewById<DrawingView>(R.id.drawingView)
            initialZoom = drawingView.getViewport().first
        }
        
        // Perform pinch open
        uiDrawingView.pinchOpen(0.8f)
        Thread.sleep(1000)

        scenario.onActivity { activity ->
            val drawingView = activity.findViewById<DrawingView>(R.id.drawingView)
            val newZoom = drawingView.getViewport().first
            
            // Note: In MainActivity.kt, drawingView.viewportGesturesEnabled is set to false when rapidOn is true.
            // So we expect the zoom to remain at initialZoom.
            assertEquals("Zoom should NOT change when Rapid is ON (gestures disabled)", initialZoom, newZoom, 0.01f)
        }
        scenario.close()
    }
}
