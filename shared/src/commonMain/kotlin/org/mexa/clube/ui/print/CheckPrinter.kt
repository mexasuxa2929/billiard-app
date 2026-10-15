package org.mexa.clube.ui.print

expect object CheckPrinter {
    fun print(text: String)
    fun saveToFile(text: String, fileName: String): String
}
