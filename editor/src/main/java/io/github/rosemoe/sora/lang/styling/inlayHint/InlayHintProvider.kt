package io.github.rosemoe.sora.lang.styling.inlayHint

/**
 * Provider for inlay hints
 */
fun interface InlayHintProvider {
    /**
     * Provide inlay hints to the given container
     */
    fun provideInlayHints(container: InlayHintsContainer)
}
