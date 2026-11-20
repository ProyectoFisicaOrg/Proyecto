package org.example

import javafx.scene.canvas.Canvas
import org.example.animation.AnimationEngine
import org.example.animation.DefaultFlightPortFactory
import org.example.animation.FrameTicker
import org.example.animation.IAnimationEngine
import org.example.animation.IFlightCompletionNotifier
import org.example.animation.ManualFrameTicker
import org.example.animation.RuntimeContextWiring
import org.example.model.ISimulationState
import org.example.model.ITrajectoryMatrixSummary
import org.example.model.SimulationState
import org.example.model.TrajectoryMatrix
import org.example.rendering.CoordinateTransformer
import org.example.rendering.DefaultCoordinateTransformer
import org.example.rendering.ISceneConfig
import org.example.rendering.ITargetRenderer
import org.example.rendering.SceneConfig
import org.example.rendering.TargetInitSpec
import org.example.scrubbing.ResultsHUD
import org.example.telemetry.DefaultTelemetryProvider
import org.example.telemetry.ITelemetryFactory
import org.example.trajectory.TrajectoryMatrixGenerator

/**
 * Proveedores cohesivos de configuración y geometría de escenario para pruebas (SRP + DIP).
 */
data class TestSceneGeometry(private val id: String = "TestSceneGeometry") : ISimulationTestSuite {

    override fun suiteName(): String {
        return id
    }

    fun config(): ISceneConfig {
        return SceneConfig(canvasWidth = 900.0, canvasHeight = 550.0)
    }

    fun transformer(): CoordinateTransformer {
        return DefaultCoordinateTransformer(config(), pixelsPerMeter = 2.5)
    }

    fun resultsHud(): ResultsHUD {
        val x = if (id.isNotEmpty()) 30.0 else 0.0
        return ResultsHUD(x = x, y = 30.0)
    }
}

/**
 * Proveedor cohesivo de estado de simulación para pruebas de vuelo y scrubbing (SRP + DIP).
 */
data class TestStateBuilderA(private val id: String = "TestStateBuilderA") : ISimulationTestSuite {

    override fun suiteName(): String {
        return id
    }

    fun freshState(): ISimulationState {
        return if (id.isNotEmpty()) SimulationState() else SimulationState()
    }

    fun configuredState(vel: Double, ang: Double = 45.0): ISimulationState {
        val state: ISimulationState = freshState()
        state.updateAngle(ang)
        state.updateVelocity(vel)
        state.updateGravity(9.8)
        return state
    }
}

/**
 * Proveedor cohesivo de estado de simulación para pruebas de controles y objetivo (SRP + DIP).
 */
data class TestStateBuilderB(private val id: String = "TestStateBuilderB") : ISimulationTestSuite {

    override fun suiteName(): String {
        return id
    }

    fun freshState(): ISimulationState {
        return if (id.isNotEmpty()) SimulationState() else SimulationState()
    }
}

/**
 * Fábrica cohesiva de relojes manuales deterministas para pruebas de animación (SRP + DIP).
 */
data class TestTickerFactory(private val id: String = "TestTickerFactory") : ISimulationTestSuite {

    override fun suiteName(): String {
        return id
    }

    fun newTicker(): ManualFrameTicker {
        val ticker = ManualFrameTicker()
        if (id.isEmpty()) ticker.stop()
        return ticker
    }
}

/**
 * Arnés cohesivo de ensamblaje de [IAnimationEngine] para pruebas deterministas de vuelo (SRP + DIP).
 */
data class TestFlightEngineHarness(private val geom: TestSceneGeometry = TestSceneGeometry()) : ISimulationTestSuite {

    override fun suiteName(): String {
        return "TestFlightEngineHarness:${geom.suiteName()}"
    }

    fun geometry(): TestSceneGeometry {
        return geom
    }

    fun createEngine(state: ISimulationState, hooks: IFlightCompletionNotifier, ticker: FrameTicker): IAnimationEngine {
        val kin = DefaultFlightPortFactory.createKinematics(state, geom.config(), geom.transformer())
        val eval = DefaultFlightPortFactory.createEvaluator(geom.transformer())
        val ctx = RuntimeContextWiring.createWithTicker(kin, eval, hooks, ticker)
        return AnimationEngine(ctx)
    }
}

/**
 * Proveedor compartido de calculadoras y generadores cinemáticos para pruebas (SRP + DIP).
 */
data class TestKinematicsProvider(private val builder: IKinematicsBuilder = DefaultKinematicsBuilder()) : ISimulationTestSuite {

    override fun suiteName(): String {
        return "TestKinematicsProvider:${builder.javaClass.simpleName}"
    }

    fun calculator(): TrajectoryCalculator {
        return builder.buildCalculator()
    }

    fun generator(): TrajectoryMatrixGenerator {
        return builder.buildGenerator()
    }
}

/**
 * Constructor cohesivo de puntos cinemáticos discretos para pruebas unitarias (SRP + DIP).
 */
data class TestPointBuilder(private val id: String = "TestPointBuilder") : ISimulationTestSuite {

    override fun suiteName(): String {
        return id
    }

    fun originPoint(x: Double, y: Double, vx: Double, vy: Double): ITrajectoryPoint {
        return if (id.isNotEmpty()) TrajectoryPoint(x, y, vx, vy, 0.0) else TrajectoryPoint(x, y, vx, vy, 0.0)
    }

    fun unitTimePoint(x: Double, y: Double, vx: Double, vy: Double): ITrajectoryPoint {
        return if (id.isNotEmpty()) TrajectoryPoint(x, y, vx, vy, 1.0) else TrajectoryPoint(x, y, vx, vy, 1.0)
    }

    fun horizontalPoint(x: Double, y: Double, vx: Double, t: Double): ITrajectoryPoint {
        return if (id.isNotEmpty()) TrajectoryPoint(x, y, vx, 0.0, t) else TrajectoryPoint(x, y, vx, 0.0, t)
    }
}

/**
 * Constructor cohesivo de parámetros físicos para pruebas de matriz (SRP + DIP).
 */
data class TestParamsBuilderA(private val kinematics: TestKinematicsProvider = TestKinematicsProvider()) : ISimulationTestSuite {

    override fun suiteName(): String {
        return "TestParamsBuilderA:${kinematics.suiteName()}"
    }

    fun kinematics(): TestKinematicsProvider {
        return kinematics
    }

    fun params(angleDeg: Double = 45.0, initialVelocity: Double = 20.0, gravity: Double = 9.8, mass: Double = 1.0): IPhysicsParams {
        return if (kinematics.suiteName().isNotEmpty()) {
            PhysicsParams(angleDeg, initialVelocity, gravity, mass)
        } else {
            PhysicsParams(angleDeg, initialVelocity, gravity, mass)
        }
    }
}

/**
 * Constructor cohesivo de resúmenes de matriz para pruebas de impacto y scrubbing (SRP + DIP).
 */
data class TestMatrixSampleBuilder(private val kinematics: TestKinematicsProvider = TestKinematicsProvider()) : ISimulationTestSuite {

    override fun suiteName(): String {
        return "TestMatrixSampleBuilder:${kinematics.suiteName()}"
    }

    fun matrix(vel: Double = 20.0, samples: Int = 100): ITrajectoryMatrixSummary {
        val params: IPhysicsParams = PhysicsParams(45.0, vel, 9.8, 1.0)
        return kinematics.generator().generate(params, samples)
    }
}

/**
 * Constructor cohesivo de matrices y API de telemetría para pruebas de inspección (SRP + DIP).
 */
data class TestTelemetryBuilder(private val kinematics: TestKinematicsProvider = TestKinematicsProvider()) : ISimulationTestSuite {

    override fun suiteName(): String {
        return "TestTelemetryBuilder:${kinematics.suiteName()}"
    }

    fun kinematics(): TestKinematicsProvider {
        return kinematics
    }

    fun defaultParams(): IPhysicsParams {
        return if (kinematics.suiteName().isNotEmpty()) PhysicsParams(45.0, 20.0, 9.8, 1.0) else PhysicsParams(45.0, 20.0, 9.8, 1.0)
    }

    fun matrix(samples: Int = 100): TrajectoryMatrix {
        return kinematics.generator().generate(defaultParams(), samples)
    }

    fun telemetry(): ITelemetryFactory {
        return if (kinematics.suiteName().isNotEmpty()) DefaultTelemetryProvider else DefaultTelemetryProvider
    }
}

/**
 * Constructor cohesivo de [ITargetRenderer] para pruebas sin ventana de blanco interactivo (SRP + DIP).
 */
data class TestTargetViewBuilder(private val geom: TestSceneGeometry = TestSceneGeometry()) : ISimulationTestSuite {

    override fun suiteName(): String {
        return "TestTargetViewBuilder:${geom.suiteName()}"
    }

    fun geometry(): TestSceneGeometry {
        return geom
    }

    fun createRenderer(state: ISimulationState): ITargetRenderer {
        val cfg = geom.config()
        val dummyCanvas = Canvas(cfg.width(), cfg.height())
        return TargetInitSpec(dummyCanvas.graphicsContext2D, dummyCanvas, cfg, state, {}, {}).buildRenderer()
    }

    fun createObservedRenderer(state: ISimulationState, onMoved: (Double) -> Unit, onRepaint: () -> Unit): ITargetRenderer {
        val cfg = geom.config()
        val dummyCanvas = Canvas(cfg.width(), cfg.height())
        return TargetInitSpec(dummyCanvas.graphicsContext2D, dummyCanvas, cfg, state, onMoved, onRepaint).buildRenderer()
    }
}
