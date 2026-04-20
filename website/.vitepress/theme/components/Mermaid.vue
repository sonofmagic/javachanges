<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

const props = defineProps<{
  graph: string
}>()

const svg = ref('')
const error = ref('')

async function renderDiagram(source: string) {
  if (typeof window === 'undefined') {
    return
  }

  try {
    const { default: mermaid } = await import('mermaid')

    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'antiscript',
      theme: document.documentElement.classList.contains('dark') ? 'dark' : 'default',
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
}

onMounted(() => {
  void renderDiagram(props.graph)
})

watch(() => props.graph, (nextGraph) => {
  void renderDiagram(nextGraph)
})
</script>

<template>
  <div class="vp-mermaid" data-mermaid>
    <div v-if="svg" class="vp-mermaid__diagram" v-html="svg" />
    <pre v-else-if="error" class="vp-mermaid__error">{{ error }}</pre>
    <pre v-else class="vp-mermaid__source">{{ graph }}</pre>
  </div>
</template>
