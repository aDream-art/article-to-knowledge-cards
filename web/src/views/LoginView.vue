<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import BaseIcon from '@/components/BaseIcon.vue'
import { useAuthStore } from '@/stores/auth'

type Mode = 'login' | 'register'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const mode = ref<Mode>('login')
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  confirmPassword: '',
})

const isRegister = computed(() => mode.value === 'register')

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度 3–32 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6–64 位', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 32, message: '昵称最多 32 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (!isRegister.value) {
          callback()
          return
        }
        if (!value) {
          callback(new Error('请再次输入密码'))
          return
        }
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const features = [
  { icon: 'chat', title: '会话隔离', desc: '每个会话独立上下文，多主题互不串扰' },
  { icon: 'file', title: '文档问答', desc: '上传 TXT 后基于原文片段回答' },
  { icon: 'bookmark', title: '收藏与标签', desc: '好回答留存为快照，随时检索' },
]

function switchMode(next: Mode): void {
  if (mode.value === next) return
  mode.value = next
  void nextTick(() => formRef.value?.clearValidate())
}

async function handleSubmit(): Promise<void> {
  const instance = formRef.value
  if (!instance || loading.value) return

  const valid = await instance
    .validate()
    .then(() => true)
    .catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const username = form.username.trim()
    if (isRegister.value) {
      await auth.register({
        username,
        password: form.password,
        nickname: form.nickname.trim() || username,
      })
      await auth.login({ username, password: form.password })
      ElMessage.success('注册成功，欢迎使用轻问 AI')
    } else {
      await auth.login({ username, password: form.password })
      ElMessage.success('登录成功')
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/chat'
    await router.replace(redirect)
  } catch {
    // 拦截器已给出可读提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="auth-visual">
      <div class="visual-glow glow-a" />
      <div class="visual-glow glow-b" />

      <div class="visual-content">
        <div class="visual-brand">
          <span class="brand-mark">问</span>
          <div class="brand-copy">
            <strong>轻问 AI</strong>
            <span>轻量 AI 问答助手</span>
          </div>
        </div>

        <h1 class="visual-title">
          把问题交给它，<br />
          把上下文留给你。
        </h1>
        <p class="visual-sub">
          一个入口提问，按会话隔离上下文；上传 TXT 就能问文档；好回答收藏成快照，随时找回。
        </p>

        <ul class="feature-list">
          <li v-for="item in features" :key="item.title">
            <span class="feature-icon"><BaseIcon :name="item.icon" :size="16" /></span>
            <div class="feature-copy">
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </div>
          </li>
        </ul>

        <div class="visual-foot">
          <span>Vue 3 + Vite</span>
          <i />
          <span>SSE 流式</span>
          <i />
          <span>Spring Boot 3.5</span>
        </div>
      </div>
    </section>

    <section class="auth-form-side">
      <div class="auth-card">
        <header class="auth-head">
          <span class="auth-eyebrow">{{ isRegister ? 'CREATE ACCOUNT' : 'SIGN IN' }}</span>
          <h2>{{ isRegister ? '创建你的账号' : '欢迎回来' }}</h2>
          <p>{{ isRegister ? '注册后即可开始提问、收藏与查看统计' : '登录后继续之前的会话与收藏' }}</p>
        </header>

        <div class="mode-switch" role="tablist">
          <button
            type="button"
            :class="{ active: mode === 'login' }"
            role="tab"
            :aria-selected="mode === 'login'"
            @click="switchMode('login')"
          >
            登录
          </button>
          <button
            type="button"
            :class="{ active: mode === 'register' }"
            role="tab"
            :aria-selected="mode === 'register'"
            @click="switchMode('register')"
          >
            注册
          </button>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          class="auth-form"
          @submit.prevent="handleSubmit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="3–32 位用户名" autocomplete="username">
              <template #prefix><BaseIcon name="user" :size="15" /></template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="isRegister" label="昵称" prop="nickname">
            <el-input v-model="form.nickname" placeholder="展示在页面右上角" maxlength="32">
              <template #prefix><BaseIcon name="user" :size="15" /></template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="至少 6 位"
              :autocomplete="isRegister ? 'new-password' : 'current-password'"
            >
              <template #prefix><BaseIcon name="lock" :size="15" /></template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="isRegister" label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              show-password
              placeholder="再次输入密码"
              autocomplete="new-password"
            >
              <template #prefix><BaseIcon name="lock" :size="15" /></template>
            </el-input>
          </el-form-item>

          <el-button type="primary" class="submit-btn" :loading="loading" native-type="submit">
            {{ isRegister ? '注册并登录' : '登录' }}
            <BaseIcon v-if="!loading" name="chevron-right" :size="14" />
          </el-button>
        </el-form>

        <p class="auth-note">
          <BaseIcon name="sparkles" :size="13" />
          无需邮箱验证，注册后直接开始使用
        </p>
      </div>

      <footer class="auth-footer">轻问 AI · 会话隔离 / 文档问答 / 收藏统计</footer>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(0, 0.92fr);
  min-height: 100vh;
  background: #fff;
}

/* ----------------------------- 左侧品牌区 ----------------------------- */

.auth-visual {
  position: relative;
  overflow: hidden;
  padding: 56px 60px 44px;
  color: #fff;
  background: radial-gradient(circle at 12% 8%, #5b5bf0 0%, transparent 46%),
    radial-gradient(circle at 88% 92%, #7c3aed 0%, transparent 52%),
    linear-gradient(155deg, #141634 0%, #1e1b4b 46%, #2a2168 100%);
}

.visual-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  opacity: 0.55;
  pointer-events: none;
}

.glow-a {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -90px;
  background: radial-gradient(circle, rgba(129, 140, 248, 0.5), transparent 65%);
}

.glow-b {
  width: 320px;
  height: 320px;
  bottom: -140px;
  left: -80px;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.42), transparent 65%);
}

.visual-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 520px;
}

.visual-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.24);
  font-size: 18px;
  font-weight: 700;
  backdrop-filter: blur(6px);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.brand-copy strong {
  font-size: 16px;
  letter-spacing: 0.02em;
}

.brand-copy span {
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.62);
}

.visual-title {
  margin: 64px 0 16px;
  font-size: clamp(28px, 3vw, 40px);
  font-weight: 750;
  line-height: 1.28;
  letter-spacing: -0.015em;
}

.visual-sub {
  max-width: 430px;
  font-size: 14px;
  line-height: 1.85;
  color: rgba(255, 255, 255, 0.72);
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 44px 0 0;
  padding: 0;
  list-style: none;
}

.feature-list li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.feature-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: #c7d2fe;
}

.feature-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feature-copy strong {
  font-size: 13.5px;
  font-weight: 600;
}

.feature-copy span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.58);
}

.visual-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: auto;
  padding-top: 48px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.46);
  letter-spacing: 0.03em;
}

.visual-foot i {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.32);
}

/* ----------------------------- 右侧表单区 ----------------------------- */

.auth-form-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 32px 28px;
  background: #fff;
}

.auth-card {
  width: 100%;
  max-width: 384px;
}

.auth-eyebrow {
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--primary);
}

.auth-head h2 {
  margin-top: 10px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-strong);
}

.auth-head p {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-muted);
}

.mode-switch {
  display: flex;
  gap: 4px;
  margin: 26px 0 22px;
  padding: 4px;
  border-radius: 11px;
  background: var(--bg-subtle);
  border: 1px solid var(--border);
}

.mode-switch button {
  flex: 1;
  padding: 8px 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.18s ease;
}

.mode-switch button.active {
  background: #fff;
  color: var(--primary-strong);
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.auth-form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  font-size: 12.5px;
  font-weight: 550;
  color: var(--text-secondary);
}

.auth-form :deep(.el-input__prefix) {
  color: var(--text-muted);
}

.submit-btn {
  width: 100%;
  height: 44px;
  margin-top: 6px;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 11px;
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.24);
}

.auth-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 18px;
  font-size: 11.5px;
  color: var(--text-muted);
}

.auth-footer {
  margin-top: 40px;
  font-size: 11px;
  color: #a7adbd;
}

@media (max-width: 1000px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    display: none;
  }

  .auth-form-side {
    min-height: 100vh;
    background: linear-gradient(180deg, #f7f7ff 0%, #fff 42%);
  }
}

@media (max-width: 480px) {
  .auth-form-side {
    padding: 32px 20px;
  }
}
</style>
