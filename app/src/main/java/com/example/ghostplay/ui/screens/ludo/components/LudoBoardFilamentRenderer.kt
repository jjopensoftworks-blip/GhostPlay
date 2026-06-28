package com.example.ghostplay.ui.screens.ludo.components

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.MaterialProvider
import com.google.android.filament.gltfio.UbershaderProvider

class LudoBoardFilamentRenderer(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    
    private var engine: Engine? = null
    private var renderer: Renderer? = null
    private var scene: Scene? = null
    private var view: View? = null
    private var camera: Camera? = null
    private var swapChain: SwapChain? = null
    
    private var assetLoader: AssetLoader? = null
    private var materialProvider: MaterialProvider? = null

    private var surfaceCreated = false
    private var uiHelper: android.view.Choreographer? = null

    init {
        holder.addCallback(this)
        
        // Initialize Filament system library
        Filament.init()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val engine = Engine.create().also { this.engine = it }
        val renderer = engine.createRenderer().also { this.renderer = it }
        val scene = engine.createScene().also { this.scene = it }
        val view = engine.createView().also { this.view = it }
        val camera = engine.createCamera(engine.entityManager.create()).also { this.camera = it }

        // Setup camera viewport and transform projection matrix
        camera.setLensProjection(45.0, width.toDouble() / height.toDouble(), 0.1, 100.0)
        camera.lookAt(0.0, 10.0, 15.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
        
        view.scene = scene
        view.camera = camera
        view.setViewport(Viewport(0, 0, width, height))

        // Create swap chain from surface holder
        val swapChain = engine.createSwapChain(holder.surface).also { this.swapChain = it }
        
        // Create Filament asset loader
        val materialProvider = UbershaderProvider(engine).also { this.materialProvider = it }
        this.assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())

        setupSceneLights(engine, scene)
        
        surfaceCreated = true
        startRenderLoop()
    }

    private fun setupSceneLights(engine: Engine, scene: Scene) {
        // Create direct sun light with dynamic shadows
        val sun = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.95f, 0.9f)
            .intensity(80000.0f)
            .direction(0.0f, -1.0f, -0.5f)
            .castShadows(true)
            .build(engine, sun)
        
        scene.addEntity(sun)
    }

    private val frameCallback = object : android.view.Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (surfaceCreated) {
                renderFrame(frameTimeNanos)
                uiHelper?.postFrameCallback(this)
            }
        }
    }

    private fun startRenderLoop() {
        uiHelper = android.view.Choreographer.getInstance()
        uiHelper?.postFrameCallback(frameCallback)
    }

    private fun stopRenderLoop() {
        uiHelper?.removeFrameCallback(frameCallback)
        uiHelper = null
    }

    private fun renderFrame(frameTimeNanos: Long) {
        val renderer = this.renderer ?: return
        val swapChain = this.swapChain ?: return
        val view = this.view ?: return
        
        if (renderer.beginFrame(swapChain, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        view?.setViewport(Viewport(0, 0, width, height))
        camera?.setLensProjection(45.0, width.toDouble() / height.toDouble(), 0.1, 100.0)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceCreated = false
        stopRenderLoop()
        
        val engine = this.engine ?: return
        
        // Destroy components to release native buffers
        assetLoader?.destroy()
        materialProvider?.destroy()
        swapChain?.let { engine.destroySwapChain(it) }
        view?.let { engine.destroyView(it) }
        scene?.let { engine.destroyScene(it) }
        camera?.let { engine.destroyCameraComponent(it.entity) }
        engine.destroy()
        
        this.engine = null
        this.renderer = null
        this.scene = null
        this.view = null
        this.camera = null
        this.swapChain = null
        this.assetLoader = null
        this.materialProvider = null
    }
}
