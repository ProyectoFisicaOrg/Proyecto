package org.example

import javafx.scene.canvas.Canvas
import org.example.animation.DefaultAnimationEngineFactory
import org.example.animation.DefaultFlightPortFactory
import org.example.animation.FrameTicker
import org.example.animation.IAnimationEngine
import org.example.animation.IFlightCompletionNotifier
import org.example.model.ISimulationState
import org.example.model.ITrajectoryMatrixSummary
import org.example.model.SimulationState
import org.example.model.TrajectoryMatrix
import org.example.rendering.CoordinateTransformer
import org.example.rendering.DefaultCoordinateTransformer
import org.example.rendering.ISceneConfig
import org.example.rendering.ITargetRenderer
import org.example.rendering.SceneConfig
import org.example.rendering.TargetRenderer
import org.example.telemetry.DefaultTelemetryProvider
import org.example.telemetry.ITelemetryFactory
import org.example.trajectory.TrajectoryMatrixGenerator

/**
 * Proveedores cohesivos de configuración y geometría de escenario para pruebas (SRP + DIP).
 */
class TestSceneGeometry : ISimulationTestSuite {

    override fun suiteName(): String {
        return "TestSceneGeometry"
    }

    fun config(): ISceneConfig {
        return SceneConfig(canvasWidth = 900.0, canvasHeight = 550.0)
    }

    fun transformer(): CoordinateTransformer {
        return DefaultCoordinateTransformer(config(), pixelsPerMeter = 2.5)
    }
}

/**
 * Proveedor cohesivo de estado de simulación para pruebas de vuelo y scrubbing (SRP + DIP).
 */
class TestStateBuilderA : ISimulationTestSuite {

    override fun suiteName(): String {
        return "TestStateBuilderA"
    }

    fun freshState(): ISimulationState {
        return SimulationState()
    }

    fun configuredState(vel: Double, ang: Double = 45.0): ISimulationState {
        val state: ISimulationState = SimulationState()
        state.angle = ang
        state.velocity = vel
        state.gravity = 9.8
        return state
    }
}

/**
 * Proveedor cohesivo de estado de simulación para pruebas de controles y objetivo (SRP + DIP).
 */
class TestStateBuilderB : ISimulationTestSuite {

    override fun suiteName(): String {
        return "TestStateBuilderB"
    }

    fun freshState(): ISimulationState {
        return SimulationState()
    }
}

/**
 * Arnés cohesivo de ensamblaje de [IAnimationEngine] para pruebas deterministas de vuelo (SRP + DIP).
 */
class TestFlightEngineHarness : ISimulationTestSuite {

    private val geom: TestSceneGeometry = TestSceneGeometry()

    override fun suiteName(): String {
        return "TestFlightEngineHarness"
    }

    fun geometry(): TestSceneGeometry {
        return geom
    }

    fun createEngine(state: ISimulationState, hooks: IFlightCompletionNotifier, ticker: FrameTicker): IAnimationEngine {
        val ports = DefaultFlightPortFactory()
        val kin = ports.createKinematics(state, geom.config(), geom.transformer())
        val eval = ports.createEvaluator(geom.transformer())
        return DefaultAnimationEngineFactory().createWithTicker(kin, eval, hooks, ticker)
    }
}

/**
 * Proveedor compartido de calculadoras y generadores cinemáticos para pruebas (SRP + DIP).
 */
class TestKinematicsProvider : ISimulationTestSuite {

    private val builder: IKinematicsBuilder = DefaultKinematicsBuilder()

    override fun suiteName(): String {
        return "TestKinematicsProvider"
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
class TestPointBuilder : ISimulationTestSuite {

    override fun suiteName(): String {
        return "TestPointBuilder"
    }

    fun originPoint(x: Double, y: Double, vx: Double, vy: Double): ITrajectoryPoint {
        return TrajectoryPoint(x, y, vx, vy, 0.0)
    }

    fun unitTimePoint(x: Double, y: Double, vx: Double, vy: Double): ITrajectoryPoint {
        return TrajectoryPoint(x, y, vx, vy, 1.0)
    }

    fun horizontalPoint(x: Double, y: Double, vx: Double, t: Double): ITrajectoryPoint {
        return TrajectoryPoint(x, y, vx, 0.0, t)
    }
}

/**
 * Constructor cohesivo de parámetros físicos para pruebas de matriz (SRP + DIP).
 */
class TestParamsBuilderA : ISimulationTestSuite {

    private val kinematics: TestKinematicsProvider = TestKinematicsProvider()

    override fun suiteName(): String {
        return "TestParamsBuilderA"
    }

    fun kinematics(): TestKinematicsProvider {
        return kinematics
    }

    fun params(angleDeg: Double = 45.0, initialVelocity: Double = 20.0, gravity: Double = 9.8, mass: Double = 1.0): IPhysicsParams {
        return PhysicsParams(angleDeg, initialVelocity, gravity, mass)
    }
}

/**
 * Constructor cohesivo de resúmenes de matriz para pruebas de impacto y scrubbing (SRP + DIP).
 */
class TestMatrixSampleBuilder : ISimulationTestSuite {

    private val kinematics: TestKinematicsProvider = TestKinematicsProvider()

    override fun suiteName(): String {
        return "TestMatrixSampleBuilder"
    }

    fun matrix(vel: Double = 20.0, samples: Int = 100): ITrajectoryMatrixSummary {
        val params: IPhysicsParams = PhysicsParams(45.0, vel, 9.8, 1.0)
        return kinematics.generator().generate(params, samples)
    }
}

/**
 * Constructor cohesivo de matrices y API de telemetría para pruebas de inspección (SRP + DIP).
 */
class TestTelemetryBuilder : ISimulationTestSuite {

    private val kinematics: TestKinematicsProvider = TestKinematicsProvider()

    override fun suiteName(): String {
        return "TestTelemetryBuilder"
    }

    fun kinematics(): TestKinematicsProvider {
        return kinematics
    }

    fun defaultParams(): IPhysicsParams {
        return PhysicsParams(45.0, 20.0, 9.8, 1.0)
    }

    fun matrix(samples: Int = 100): TrajectoryMatrix {
        return kinematics.generator().generate(defaultParams(), samples)
    }

    fun telemetry(): ITelemetryFactory {
        return DefaultTelemetryProvider()
    }
}

/**
 * Constructor cohesivo de [ITargetRenderer] para pruebas sin ventana de blanco interactivo (SRP + DIP).
 */
class TestTargetViewBuilder : ISimulationTestSuite {

    private val geom: TestSceneGeometry = TestSceneGeometry()

    override fun suiteName(): String {
        return "TestTargetViewBuilder"
    }

    fun geometry(): TestSceneGeometry {
        return geom
    }

    fun createRenderer(state: ISimulationState): ITargetRenderer {
        val cfg = geom.config()
        val dummyCanvas = Canvas(cfg.canvasWidth, cfg.canvasHeight)
        return TargetRenderer(dummyCanvas.graphicsContext2D, cfg, dummyCanvas, state, {}, {})
    }

    fun createObservedRenderer(state: ISimulationState, onMoved: (Double) -> Unit, onRepaint: () -> Unit): ITargetRenderer {
        val cfg = geom.config()
        val dummyCanvas = Canvas(cfg.canvasWidth, cfg.canvasHeight)
        return TargetRenderer(dummyCanvas.graphicsContext2D, cfg, dummyCanvas, state, onMoved, onRepaint)
    }
}
