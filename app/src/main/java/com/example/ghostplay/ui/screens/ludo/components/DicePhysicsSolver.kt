package com.example.ghostplay.ui.screens.ludo.components

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class PhysicsState(
    val px: Float, val py: Float, val pz: Float, // Position
    val vx: Float, val vy: Float, val vz: Float, // Linear velocity
    val qx: Float, val qy: Float, val qz: Float, val qw: Float, // Quaternion orientation
    val wx: Float, val wy: Float, val wz: Float  // Angular velocity
)

class DicePhysicsSolver {
    
    // Simulates a physics roll with a starting impulse
    fun simulateRoll(startingImpulseForce: Float): List<PhysicsState> {
        val states = mutableListOf<PhysicsState>()
        
        // Initial state
        var px = 0f
        var py = 3f // Height above pedestal base
        var pz = 0f
        
        // Random starting velocities (tumbling)
        var vx = Random.nextFloat() * 4f - 2f
        var vy = startingImpulseForce // Upward bounce
        var vz = Random.nextFloat() * 4f - 2f
        
        var qx = 0f
        var qy = 0f
        var qz = 0f
        var qw = 1f
        
        var wx = Random.nextFloat() * 15f + 5f
        var wy = Random.nextFloat() * 15f + 5f
        var wz = Random.nextFloat() * 15f + 5f

        val dt = 0.016f // 60 FPS time step
        val gravity = -9.8f
        val restitution = 0.6f // Bounce bounciness
        
        // Run simulation until it settles to rest on the pedestal (y = 0)
        var elapsed = 0f
        while (elapsed < 2.0f) { // Max 2 seconds of physics simulation
            // Apply gravity
            vy += gravity * dt
            
            // Update position
            px += vx * dt
            py += vy * dt
            pz += vz * dt
            
            // Pedestal collision check (floor y = 0)
            if (py <= 0f) {
                py = 0f
                vy = -vy * restitution // Reverse velocity & dampen
                vx *= 0.7f // Friction
                vz *= 0.7f
                
                // Slow down angular spin on impact
                wx *= 0.6f
                wy *= 0.6f
                wz *= 0.6f
            }
            
            // Update orientation quaternions based on angular velocity
            val angle = Math.sqrt((wx * wx + wy * wy + wz * wz).toDouble()).toFloat()
            if (angle > 0.001f) {
                val s = sin(angle * dt * 0.5f) / angle
                val dqx = wx * s
                val dqy = wy * s
                val dqz = wz * s
                val dqw = cos(angle * dt * 0.5f).toFloat()
                
                // Quaternion multiplication (q = q * dq)
                val nqx = qw * dqx + qx * dqw + qy * dqz - qz * dqy
                val nqy = qw * dqy - qx * dqz + qy * dqw + qz * dqx
                val nqz = qw * dqz + qx * dqy - qy * dqx + qz * dqw
                val nqw = qw * dqw - qx * dqx - qy * dqy - qz * dqz
                
                // Normalize quaternion
                val len = Math.sqrt((nqx * nqx + nqy * nqy + nqz * nqz + nqw * nqw).toDouble()).toFloat()
                qx = nqx / len
                qy = nqy / len
                qz = nqz / len
                qw = nqw / len
            }
            
            states.add(PhysicsState(px, py, pz, vx, vy, vz, qx, qy, qz, qw, wx, wy, wz))
            elapsed += dt
            
            // Break early if resting
            if (Math.abs(vy) < 0.05f && py == 0f && angle < 0.1f) {
                break
            }
        }
        return states
    }
}
