<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import BaseIcon from './BaseIcon.vue'
import { useFavoriteStore } from '@/stores/favorite'
import { useTagStore } from '@/stores/tag'
import { PROVIDER_LABELS } from '@/utils/constants'

const props = defineProps<{
  visible: boolean
  sessionId: number | null
  question: string
  answer: string
  provider: string | null
  model: string | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const favorite = useFavoriteStore()
const tagStore = useTagStore()

const selected = ref<Array<number | string>>([])
const saving = ref(false)

const visible = computed({
  get: () => props.visible,
  set: (next: boolean) => emit('update:visible', next),
})

const providerLabel = computed(() =>
  props.provider ? (PROVIDER_LABELS[props.provider] ?? props.provider) : '未记录',
)

watch(
  () => props.visible,
  async (open) => {
    if (!open) return
    selected.value = []
    await tagStore.loadTags(true)
  },
)

async function submit(): Promise<void> {
  if (saving.value || !props.question.trim() || !props.answer.trim()) return
  saving.value = true
  try {
    const tagIds: number[] = []
    for (const value of selected.value) {
      if (typeof value === 'number') {
        tagIds.push(value)
        continue
      }
      const created = await tagStore.createTag(String(value))
      if (created && created.id > 0) tagIds.push(created.id)
    }

    await favorite.createFavorite({
      sessionId: props.sessionId,
      question: props.question,
      answer: props.answer,
      provider: props.provider,
      model: props.model,
      tagIds,
    })
    ElMessage.success('已加入收藏')
    visible.value = false
    emit('saved')
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="收藏这一轮问答" width="540px" :close-on-click-modal="false">
    <div class="fav-dialog">
      <section class="block">
        <div class="block-label">
          <span class="badge badge-q">问</span>
          提问
        </div>
        <p class="question">{{ question }}</p>
      </section>

      <section class="block">
        <div class="block-label">
          <span class="badge badge-a">答</span>
          回答快照
          <span class="char-count">{{ answer.length }} 字</span>
        </div>
        <div class="answer-preview">{{ answer }}</div>
      </section>

      <section class="block">
        <div class="block-label">
          <BaseIcon name="tag" :size="13" />
          标签（可新建）
        </div>
        <el-select
          v-model="selected"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="选择已有标签，或输入后回车新建"
          style="width: 100%"
        >
          <el-option v-for="tag in tagStore.tags" :key="tag.id" :label="tag.name" :value="tag.id" />
        </el-select>
        <p class="meta-line">
          来源：{{ providerLabel }}<template v-if="model"> · {{ model }}</template>
          <span class="dot">·</span>
          收藏为问答快照，删除原会话不影响
        </p>
      </section>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存到收藏</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.fav-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.block {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.block-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}

.badge {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 6px;
  font-size: 10.5px;
  font-weight: 700;
}

.badge-q {
  background: var(--primary-soft);
  color: var(--primary-strong);
}

.badge-a {
  background: #ecfdf3;
  color: #15803d;
}

.char-count {
  margin-left: auto;
  font-weight: 400;
  font-size: 10.5px;
  color: var(--text-muted);
}

.question {
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--bg-subtle);
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--text);
  word-break: break-word;
}

.answer-preview {
  max-height: 190px;
  overflow-y: auto;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.meta-line {
  font-size: 11px;
  color: var(--text-muted);
}

.dot {
  margin: 0 5px;
}
</style>
