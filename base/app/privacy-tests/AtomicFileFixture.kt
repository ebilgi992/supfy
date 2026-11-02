package android.util

import java.io.File
import java.io.FileOutputStream

/** Android's atomic-file boundary backed by real temporary filesystem writes for this test. */
class AtomicFile(val baseFile: File) {
    fun readFully() = baseFile.readBytes()
    fun startWrite(): FileOutputStream = FileOutputStream(baseFile)
    fun finishWrite(stream: FileOutputStream) = stream.close()
    fun failWrite(stream: FileOutputStream) = stream.close()
    fun delete() { baseFile.delete() }
}

