<script setup lang="ts">
import { computed, ref } from 'vue'
import BaseIcon from './BaseIcon.vue'

const props = defineProps<{
  modelValue: string
  streaming: boolean
  disabled: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: []
  stop: []
}>()

const inputRef = ref<{ focus: () => void } | null>(null)

const value = computed({
  get: () => props.modelValue,
  set: (next: string) => emit('update:modelValue', next),
})

const canSend = computed(() => value.value.trim().length > 0 && !props.streaming && !props.disabled)

function handleKeydown(event: KeyboardEvent): void {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) return
  event.preventDefault()
  if (canSend.value) emit('send')
}

function focus(): void {
  inputRef.value?.focus()
}

defineExpose({ focus })
</script>

<template>
  <div class="composer">
    <div class="composer-box" :class="{ 'is-disabled': disabled }">
      <el-input
        ref="inputRef"
        v-model="value"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 6 }"
        resize="none"
        class="composer-input"
        :disabled="disabled"
        :placeholder="disabled ? '请先在左侧新建或选择会话' : '输入你的问题…（Enter 发送，Shift + Enter 换行）'"
        @keydown="handleKeydown"
      />
      <div class="composer-actions">
        <el-button v-if="streaming" class="stop-btn" @click="emit('stop')">
          <BaseIcon name="stop" :size="13" />
          停止生成
        </el-button>
        <el-button v-else type="primary" class="send-btn" :disabled="!canSend" @click="emit('send')">
          <BaseIcon name="send" :size="14" />
          发送
        </el-button>
      </div>
    </div>
    <div class="composer-hint">
      <span>回答由模型生成，请自行校验关键结论</span>
      <span v-if="streaming" class="streaming-hint">
        <i class="pulse-dot" />
        正在生成…
      </span>
      <span v-else>{{ value.length }} 字</span>
    </div>
  </div>
</template>

<style scoped>
.composer {
  flex: none;
  padding: 12px 20px 16px;
  background: linear-gradient(180deg, rgba(244, 245, 250, 0) 0%, #f4f5fa 42%);
}

.composer-box {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 15px;
  background: #fff;
  box-shadow: 0 6px 22px rgba(22, 25, 42, 0.07);
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.composer-box:focus-within {
  border-color: var(--primary-light);
  box-shadow: 0 8px 26px rgba(79, 70, 229, 0.14);
}

.composer-box.is-disabled {
  background: #fafbfe;
}

.composer-input {
  flex: 1;
}

.composer-input :deep(.el-textarea__inner) {
  padding: 4px 2px;
  border: none;
  box-shadow: none;
  background: transparent;
  font-size: 14.5px;
  line-height: 1.7;
  color: var(--text);
}

.composer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 1px;
}

.send-btn {
  min-width: 82px;
  gap: 5px;
}

.stop-btn {
  min-width: 104px;
  gap: 5px;
  color: var(--danger);
  border-color: #f3c9c5;
  background: #fff6f5;
}

.stop-btn:hover {
  color: #b42318;
  border-color: #e9a9a3;
  background: #feeceb;
}

.composer-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 7px;
  padding: 0 4px;
  font-size: 11px;
  color: var(--text-muted);
}

.streaming-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--primary);
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary);
  animation: pulse 1.1s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.25;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
