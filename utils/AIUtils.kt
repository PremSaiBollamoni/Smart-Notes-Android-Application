package com.example.external.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class AIUtils(private val context: Context) {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "",
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 2048
        }
    )

    data class AnalysisResult(
        val summary: String,
        val category: String,
        val confidence: Float,
        val suggestedTags: List<String>,
        val imageDescription: String = "",
        val detectedObjects: List<String> = emptyList(),
        val textContent: String = ""
    )

    suspend fun analyzeContent(bitmap: Bitmap): AnalysisResult = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Analyze this image comprehensively and provide a detailed response in this exact JSON format:
                {
                    "imageDescription": "Detailed visual description of the image",
                    "textContent": "Any text visible in the image, including handwritten text",
                    "detectedObjects": ["list", "of", "key", "objects", "or", "elements", "seen"],
                    "summary": "Comprehensive 2-3 sentence summary of the image content",
                    "category": "CATEGORY",
                    "confidence": CONFIDENCE_SCORE,
                    "suggestedTags": ["relevant", "tags", "max", "5", "items"]
                }

                For category, use one of: Work, Personal, Study, Meeting, Task, Research, Creative, Finance, Health, Travel, Recipe, Technical, Other.
                For confidence, provide a number between 0.0 and 1.0.
                Be thorough in detecting and transcribing any text in the image.
                If there's handwritten text, make your best effort to read and include it.
            """.trimIndent()

            // Use the standard content builder API for Android
            val response = generativeModel.generateContent(
                content {
                    text(prompt)
                    image(bitmap)
                }
            ).text?.trim() ?: throw Exception("No response from AI")

            // Clean up response and parse JSON
            val jsonString = response
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```")
                .trim()

            try {
                val jsonResponse = JSONObject(jsonString)
                AnalysisResult(
                    summary = jsonResponse.optString("summary", "No summary available"),
                    category = jsonResponse.optString("category", "Uncategorized"),
                    confidence = jsonResponse.optDouble("confidence", 0.0).toFloat(),
                    suggestedTags = jsonResponse.optJSONArray("suggestedTags")?.let { array ->
                        List(array.length()) { array.getString(it) }
                    } ?: emptyList(),
                    imageDescription = jsonResponse.optString("imageDescription", ""),
                    detectedObjects = jsonResponse.optJSONArray("detectedObjects")?.let { array ->
                        List(array.length()) { array.getString(it) }
                    } ?: emptyList(),
                    textContent = jsonResponse.optString("textContent", "")
                )
            } catch (e: Exception) {
                // Fallback parsing if JSON is malformed
                val lines = response.lines()
                AnalysisResult(
                    summary = lines.firstOrNull { it.contains("summary", ignoreCase = true) }
                        ?.substringAfter(":")?.trim()?.removeSurrounding("\"", "\"") 
                        ?: "No summary available",
                    category = lines.firstOrNull { it.contains("category", ignoreCase = true) }
                        ?.substringAfter(":")?.trim()?.removeSurrounding("\"", "\"") 
                        ?: "Uncategorized",
                    confidence = 0.5f,
                    suggestedTags = emptyList(),
                    imageDescription = lines.firstOrNull { it.contains("imageDescription", ignoreCase = true) }
                        ?.substringAfter(":")?.trim()?.removeSurrounding("\"", "\"") 
                        ?: "",
                    detectedObjects = emptyList(),
                    textContent = lines.firstOrNull { it.contains("textContent", ignoreCase = true) }
                        ?.substringAfter(":")?.trim()?.removeSurrounding("\"", "\"") 
                        ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("AIUtils", "Analysis error: ${e.message}", e)
            AnalysisResult(
                summary = "Error analyzing image: ${e.message}",
                category = "Uncategorized",
                confidence = 0.0f,
                suggestedTags = emptyList(),
                imageDescription = "",
                detectedObjects = emptyList(),
                textContent = ""
            )
        }
    }

    suspend fun generateSummary(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            if (bitmap.isRecycled) {
                return@withContext "Could not analyze: Image is not available."
            }

            val analysis = analyzeContent(bitmap)
            buildString {
                append("Image Description: ").append(analysis.imageDescription).append("\n\n")
                
                if (analysis.textContent.isNotBlank()) {
                    append("Detected Text:\n").append(analysis.textContent).append("\n\n")
                }
                
                if (analysis.detectedObjects.isNotEmpty()) {
                    append("Detected Objects: ").append(analysis.detectedObjects.joinToString(", ")).append("\n\n")
                }
                
                append("Summary: ").append(analysis.summary).append("\n\n")
                
                append("Category: ").append(analysis.category)
                append(" (Confidence: ").append((analysis.confidence * 100).toInt()).append("%)")
                
                if (analysis.suggestedTags.isNotEmpty()) {
                    append("\nTags: ").append(analysis.suggestedTags.joinToString(", "))
                }
            }
        } catch (e: Exception) {
            "Error generating summary: ${e.message}"
        }
    }

    fun saveImageToInternalStorage(bitmap: Bitmap, fileName: String): String {
        val directory = File(context.filesDir, "images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return file.absolutePath
    }
}