package com.hireflow.app.domain

object ScoreCalculator {
    fun average(technical: Int, communication: Int, problemSolving: Int, cultureFit: Int): Double {
        require(listOf(technical, communication, problemSolving, cultureFit).all { it in 1..5 }) {
            "Mỗi tiêu chí phải nằm trong khoảng 1..5"
        }
        return (technical + communication + problemSolving + cultureFit) / 4.0
    }
}
