package com.pathhelper.ai.navigation
/**
* Singleton instance or companion helper for Semantic Labels.
*/
object SemanticLabels {
    fun getClassName(classId: Int): String {
        return when (classId) {
            0 -> "Person"
            1 -> "Bicycle"
            2 -> "Car"
            3 -> "Motorcycle"
            5 -> "Bus"
            7 -> "Truck"
            9 -> "Traffic light"
            10 -> "Fire hydrant"
            11 -> "Stop sign"
            13 -> "Bench"
            15 -> "Cat"
            16 -> "Dog"
            24 -> "Backpack"
            25 -> "Umbrella"
            26 -> "Handbag"
            39 -> "Bottle"
            56 -> "Chair"
            57 -> "Couch"
            58 -> "Potted plant"
            59 -> "Bed"
            60 -> "Dining table"
            61 -> "Toilet"
            62 -> "TV"
            63 -> "Laptop"
            else -> "Obstacle"
        }
    }
}
