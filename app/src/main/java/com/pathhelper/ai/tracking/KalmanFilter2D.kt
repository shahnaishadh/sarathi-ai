package com.pathhelper.ai.tracking
/**
* Coordinates Kalman Filter1 D operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Kalman Filter1 D.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class KalmanFilter1D(
    private var position: Float,
    private var velocity: Float = 0.0f
) {
    private var p00 = 1.0f
    private var p01 = 0.0f
    private var p11 = 1.0f

    private val q00 = 0.01f
    private val q11 = 0.1f
    private val rVal = 0.1f

    fun predict(dt: Float) {
        position += velocity * dt
        p00 = p00 + 2.0f * p01 * dt + p11 * dt * dt + q00
        p01 = p01 + p11 * dt
        p11 = p11 + q11
    }

    fun update(measurement: Float) {
        val s = p00 + rVal
        if (s == 0.0f) return

        val k0 = p00 / s
        val k1 = p01 / s

        val y = measurement - position
        position += k0 * y
        velocity += k1 * y

        p00 = (1.0f - k0) * p00
        p01 = (1.0f - k0) * p01
        p11 = -k1 * p01 + p11
    }

    fun getPosition(): Float = position
    fun getVelocity(): Float = velocity
}
/**
* Coordinates Kalman Filter2 D operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Kalman Filter2 D.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class KalmanFilter2D(
    x: Float,
    y: Float
) {
    private val filterX = KalmanFilter1D(x)
    private val filterY = KalmanFilter1D(y)

    fun predict(dt: Float) {
        filterX.predict(dt)
        filterY.predict(dt)
    }

    fun update(mx: Float, my: Float) {
        filterX.update(mx)
        filterY.update(my)
    }

    fun getX(): Float = filterX.getPosition()
    fun getY(): Float = filterY.getPosition()
    fun getVx(): Float = filterX.getVelocity()
    fun getVy(): Float = filterY.getVelocity()
}
