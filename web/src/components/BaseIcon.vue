<script setup lang="ts">
import { computed } from 'vue'

const ICON_PATHS: Record<string, string> = {
  chat: 'M20.5 11.5a8.5 8.5 0 0 1-12.4 7.6L3.5 20.5l1.4-4.4A8.5 8.5 0 1 1 20.5 11.5Z',
  bookmark: 'M6.5 4.5h11a1 1 0 0 1 1 1v15l-6.5-3.8L5.5 20.5v-15a1 1 0 0 1 1-1Z',
  chart: 'M4.5 19.5h15M7.5 16V9M12 16V5M16.5 16v-4.5',
  plus: 'M12 5.5v13M5.5 12h13',
  trash: 'M4.5 7.5h15M9.5 7.5V4.5h5v3M6.5 7.5l1 12h9l1-12M10.5 11v5.5M13.5 11v5.5',
  edit: 'M4 20h4L18.5 9.5a2.1 2.1 0 0 0-3-3L5 17v3Z',
  send: 'M20.5 3.5 3.5 10.6l6.4 2.6 2.6 6.4 8-16.1ZM9.9 13.2l4.6-4.6',
  stop: 'M7 7h10v10H7z',
  file: 'M13.5 3.5H7a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V9l-5.5-5.5ZM13.5 3.5V9H19',
  upload: 'M12 16.5V4M7.5 8.5 12 4l4.5 4.5M4.5 20.5h15',
  refresh: 'M19.5 11.5a7.5 7.5 0 1 1-2.2-5.3M20 3.5v5h-5',
  search: 'M11 18.5a7.5 7.5 0 1 0 0-15 7.5 7.5 0 0 0 0 15ZM21 21l-4.2-4.2',
  copy: 'M9 8.5h9.5a1 1 0 0 1 1 1V19a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V9.5a1 1 0 0 1 1-1ZM5.5 15.5H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h9.5a1 1 0 0 1 1 1v.5',
  check: 'M5 12.5l4.5 4.5L19.5 7',
  close: 'M6.5 6.5l11 11M17.5 6.5l-11 11',
  sparkles:
    'M12 3.5l1.7 5.3 5.3 1.7-5.3 1.7-1.7 5.3-1.7-5.3L5 10.5l5.3-1.7L12 3.5ZM18.5 16.5l.8 2.2 2.2.8-2.2.8-.8 2.2-.8-2.2-2.2-.8 2.2-.8.8-2.2Z',
  alert:
    'M12 8.5v5M12 17h.01M10.2 4.2 2.9 17.1a2 2 0 0 0 1.7 3h14.8a2 2 0 0 0 1.7-3L13.8 4.2a2 2 0 0 0-3.6 0Z',
  'arrow-left': 'M19 12H5M11 18l-6-6 6-6',
  clock: 'M12 20.5a8.5 8.5 0 1 0 0-17 8.5 8.5 0 0 0 0 17ZM12 7.5V12l3 1.8',
  logout:
    'M14.5 4.5h3a1.5 1.5 0 0 1 1.5 1.5v12a1.5 1.5 0 0 1-1.5 1.5h-3M10 8l-4 4 4 4M6 12h9.5',
  user: 'M20 20.5a8 8 0 0 0-16 0M12 11.5a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z',
  lock: 'M6.5 10.5h11a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1h-11a1 1 0 0 1-1-1v-8a1 1 0 0 1 1-1ZM9 10.5V7a3 3 0 0 1 6 0v3.5',
  tag: 'M4 10V4h6l9 9-6 6-9-9ZM7.5 7.5h.01',
  'chevron-down': 'M6.5 9.5 12 15l5.5-5.5',
  'chevron-right': 'M9.5 6.5 15 12l-5.5 5.5',
}

const props = withDefaults(
  defineProps<{
    name: string
    size?: number | string
    strokeWidth?: number
  }>(),
  { size: 18, strokeWidth: 1.7 },
)

const path = computed(() => ICON_PATHS[props.name] ?? ICON_PATHS.sparkles)
const pixels = computed(() => (typeof props.size === 'number' ? `${props.size}px` : props.size))
</script>

<template>
  <svg
    class="base-icon"
    :width="pixels"
    :height="pixels"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <path :d="path" />
  </svg>
</template>

<style scoped>
.base-icon {
  display: inline-block;
  flex: none;
  vertical-align: -0.15em;
}
</style>
