<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'

const props = defineProps<{
  graph: string
}>()

const svg = ref('')
const error = ref('')
const loading = ref(true)
let themeObserver: MutationObserver | undefined

function cssVar(name: string, fallback: string): string {
  if (typeof window === 'undefined') {
    return fallback
  }

  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

async function renderDiagram(source: string) {
  if (typeof window === 'undefined') {
    return
  }

  try {
    const { default: mermaid } = await import('mermaid')
    const isDark = document.documentElement.classList.contains('dark')

    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'antiscript',
      theme: 'base',
      fontFamily: cssVar('--vp-font-family-base', 'sans-serif'),
      flowchart: {
        htmlLabels: false,
        curve: 'basis',
      },
      themeVariables: {
        background: cssVar('--vp-c-bg-soft', isDark ? '#2a211c' : '#fffaf2'),
        primaryColor: cssVar('--vp-c-bg-soft', isDark ? '#2a211c' : '#fffaf2'),
        primaryBorderColor: cssVar('--vp-c-brand-1', isDark ? '#e98954' : '#b54a1d'),
        primaryTextColor: cssVar('--vp-c-text-1', isDark ? '#f4eadf' : '#1f1d19'),
        secondaryColor: cssVar('--vp-c-bg-alt', isDark ? '#17110f' : '#efe8da'),
        secondaryBorderColor: cssVar('--vp-c-brand-2', isDark ? '#f0a16e' : '#cf6435'),
        secondaryTextColor: cssVar('--vp-c-text-1', isDark ? '#f4eadf' : '#1f1d19'),
        tertiaryColor: cssVar('--vp-c-bg', isDark ? '#201915' : '#f5f1e8'),
        tertiaryBorderColor: cssVar('--vp-c-divider', isDark ? '#46342b' : '#d7cfc1'),
        tertiaryTextColor: cssVar('--vp-c-text-1', isDark ? '#f4eadf' : '#1f1d19'),
        lineColor: cssVar('--vp-c-text-2', isDark ? '#cfbeb2' : '#544f49'),
        textColor: cssVar('--vp-c-text-1', isDark ? '#f4eadf' : '#1f1d19'),
        mainBkg: cssVar('--vp-c-bg-soft', isDark ? '#2a211c' : '#fffaf2'),
        nodeBorder: cssVar('--vp-c-brand-1', isDark ? '#e98954' : '#b54a1d'),
        clusterBkg: cssVar('--vp-c-bg-alt', isDark ? '#17110f' : '#efe8da'),
        clusterBorder: cssVar('--vp-c-divider', isDark ? '#46342b' : '#d7cfc1'),
        edgeLabelBackground: cssVar('--vp-c-bg', isDark ? '#201915' : '#f5f1e8'),
      },
      themeCSS: `
        .label, .nodeLabel, .edgeLabel, .cluster-label, .label text, .nodeLabel p {
          color: ${cssVar('--vp-c-text-1', isDark ? '#f4eadf' : '#1f1d19')} !important;
          fill: ${cssVar('--vp-c-text-1', isDark ? '#f4eadf' : '#1f1d19')} !important;
        }
        .edgeLabel rect {
          fill: ${cssVar('--vp-c-bg', isDark ? '#201915' : '#f5f1e8')} !important;
          opacity: 1 !important;
        }
      `,
    })

    const id = `mermaid-${Math.random().toString(36).slice(2, 10)}`
    const { svg: nextSvg } = await mermaid.render(id, source)
    svg.value = nextSvg
    error.value = ''
  }
  catch (cause) {
    svg.value = ''
    error.value = cause instanceof Error ? cause.message : String(cause)
  }
  finally {
    loading.value = false
  }
}

onMounted(() => {
  loading.value = true
  void renderDiagram(props.graph)

  themeObserver = new MutationObserver(() => {
    loading.value = true
    svg.value = ''
    error.value = ''
    void renderDiagram(props.graph)
  })

  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['class'],
  })
})

watch(() => props.graph, (nextGraph) => {
  loading.value = true
  svg.value = ''
  error.value = ''
  void renderDiagram(nextGraph)
})

onUnmounted(() => {
  themeObserver?.disconnect()
})
</script>

<template>
  <div class="vp-mermaid" data-mermaid>
    <div v-if="svg" class="vp-mermaid__diagram" v-html="svg" />
    <pre v-else-if="error" class="vp-mermaid__error">{{ error }}</pre>
    <div v-else-if="loading" class="vp-mermaid__loading" aria-live="polite">
      <span class="vp-mermaid__loading-dot" />
      <span>Loading diagram...</span>
    </div>
  </div>
</template>
