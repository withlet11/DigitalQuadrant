/**
 * Original: https://qiita.com/seabat-dev/items/8b6fde253ef2bf7d6335
 */

package io.github.withlet11.digitalquadrant

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader


data class OssLicenseList(val licenseList: List<LibraryLicense>) {
    companion object {
        suspend fun create(context: Context): OssLicenseList {
            val licenses = loadLibraries(context).map {
                LibraryLicense(it.name, loadLicense(context, it))
            }
            return OssLicenseList(licenses)
        }

        private suspend fun loadLibraries(context: Context): List<LibraryMetadata> {
            return withContext(Dispatchers.IO) {
                val inputStream =
                    context.resources.openRawResource(R.raw.third_party_license_metadata)
                inputStream.use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
                    reader.use { bufferedReader ->
                        val libraries = mutableListOf<LibraryMetadata>()
                        while (true) {
                            val line = bufferedReader.readLine() ?: break
                            val (position, name) = line.split(' ', limit = 2)
                            val (offset, length) = position.split(':').map { it.toInt() }
                            libraries.add(LibraryMetadata(name, offset, length))
                        }
                        libraries.toList()
                    }
                }
            }
        }

        private suspend fun loadLicense(context: Context, library: LibraryMetadata): String {
            return withContext(Dispatchers.IO) {
                val charArray = CharArray(library.length)
                val inputStream =
                    context.resources.openRawResource(R.raw.third_party_licenses)
                inputStream.use { stream ->
                    val bufferReader =
                        BufferedReader(InputStreamReader(stream, "UTF-8"))
                    bufferReader.use { reader ->
                        reader.skip(library.offset.toLong())
                        reader.read(charArray, 0, library.length)
                    }
                }
                String(charArray)
            }
        }
    }
}

data class LibraryMetadata(
    val name: String,
    val offset: Int,
    val length: Int
)

data class LibraryLicense(
    val name: String,
    val licenseText: String
)
