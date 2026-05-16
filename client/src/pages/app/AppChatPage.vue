<template>
  <div id="appChatPage">
    <!-- Top bar -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || "Website Generator" }}</h1>
      </div>
      <div class="header-right">
        <a-button type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          App details
        </a-button>
        <a-button
          type="primary"
          ghost
          @click="downloadCode"
          :loading="downloading"
          :disabled="!isOwner"
        >
          <template #icon>
            <DownloadOutlined />
          </template>
          Download code
        </a-button>

        <a-button type="primary" @click="deployApp" :loading="deploying">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          Deploy
        </a-button>
      </div>
    </div>

    <!-- Main content -->
    <div class="main-content">
      <!-- Chat column -->
      <div class="chat-section">
        <!-- Messages -->
        <div class="messages-container" ref="messagesContainer">
          <!-- Load more -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
              Load more history
            </a-button>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar" />
              </div>
              <div class="message-content">
                <MarkdownRenderer v-if="message.content" :content="message.content" />
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI is thinking…</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Message input -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="You can only chat on apps you own." placement="top">
              <a-textarea
                v-model:value="userInput"
                placeholder="Describe the site you want. More detail usually gives better results."
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
              v-else
              v-model:value="userInput"
              placeholder="Describe the site you want. More detail usually gives better results."
              :rows="4"
              :maxlength="1000"
              @keydown.enter.prevent="sendMessage"
              :disabled="isGenerating"
            />
            <div class="input-actions">
              <a-button
                type="primary"
                @click="sendMessage"
                :loading="isGenerating"
                :disabled="!isOwner"
              >
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <!-- Preview column -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>Generated site preview</h3>
          <div class="preview-actions">
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              Open in new window
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="!previewUrl && !isGenerating" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>The generated site will appear here when ready.</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>Generating site…</p>
          </div>
          <iframe
            v-else
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
            @load="onIframeLoad"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- App detail modal -->
    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <!-- Deploy success modal -->
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed } from "vue"
import { useRoute, useRouter } from "vue-router"
import { message } from "ant-design-vue"
import { useLoginUserStore } from "@/stores/loginUser"
import {
  getAppVoById,
  deployApp as deployAppApi,
  deleteApp as deleteAppApi,
} from "@/api/appController"
import { listAppChatHistory } from "@/api/chatHistoryController"
import { CodeGenTypeEnum } from "@/utils/codeGenTypes"
import request from "@/request"

import MarkdownRenderer from "@/components/MarkdownRenderer.vue"
import AppDetailModal from "@/components/AppDetailModal.vue"
import DeploySuccessModal from "@/components/DeploySuccessModal.vue"
import aiAvatar from "@/assets/logo.jpg"
import { API_BASE_URL, getStaticPreviewUrl } from "@/config/env"

import {
  CloudUploadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
} from "@ant-design/icons-vue"

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// App info
const appInfo = ref<API.AppVO>()
const appId = ref<any>()

// Chat
interface Message {
  type: "user" | "ai"
  content: string
  loading?: boolean
  createTime?: string
}

const messages = ref<Message[]>([])
const userInput = ref("")
const isGenerating = ref(false)
const currentEventSource = ref<EventSource | null>(null)
const messagesContainer = ref<HTMLElement>()

// Chat history pagination
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// Preview
const previewUrl = ref("")
const previewReady = ref(false)

// Deploy
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref("")

// Permissions
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === "admin"
})

const appDetailVisible = ref(false)

const showAppDetail = () => {
  appDetailVisible.value = true
}

const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        const historyMessages: Message[] = chatHistories
          .map((chat) => ({
            type: (chat.messageType === "user" ? "user" : "ai") as "user" | "ai",
            content: chat.message || "",
            createTime: chat.createTime,
          }))
          .reverse()
        if (isLoadMore) {
          messages.value.unshift(...historyMessages)
        } else {
          messages.value = historyMessages
        }
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error("Failed to load chat history:", error)
    message.error("Failed to load chat history")
  } finally {
    loadingHistory.value = false
  }
}

const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error("App ID is missing")
    router.push("/")
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      await loadChatHistory()
      if (messages.value.length >= 2) {
        updatePreview()
      }
      if (
        appInfo.value.initPrompt &&
        isOwner.value &&
        messages.value.length === 0 &&
        historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error("Failed to load app")
      router.push("/")
    }
  } catch (error) {
    console.error("Failed to load app:", error)
    message.error("Failed to load app")
    router.push("/")
  }
}

const sendInitialMessage = async (prompt: string) => {
  messages.value.push({
    type: "user",
    content: prompt,
  })

  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: "ai",
    content: "",
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  const message = userInput.value.trim()
  userInput.value = ""

  messages.value.push({
    type: "user",
    content: message,
  })

  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: "ai",
    content: "",
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  isGenerating.value = true
  await generateCode(message, aiMessageIndex)
}

const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false

  const closeEventSource = () => {
    eventSource?.close()
    if (currentEventSource.value === eventSource) {
      currentEventSource.value = null
    }
  }

  try {
    currentEventSource.value?.close()

    const baseURL = request.defaults.baseURL || API_BASE_URL

    const params = new URLSearchParams({
      appId: appId.value || "",
      message: userMessage,
    })

    const url = `${baseURL}/app/chat/gen/code?${params}`

    eventSource = new EventSource(url, {
      withCredentials: true,
    })
    currentEventSource.value = eventSource

    let fullContent = ""

    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        if (content !== undefined && content !== null) {
          fullContent += content
          messages.value[aiMessageIndex].content = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (error) {
        console.error("Failed to parse message:", error)
        streamCompleted = true
        closeEventSource()
        handleError(error, aiMessageIndex)
      }
    }

    eventSource.addEventListener("done", function () {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      closeEventSource()

      setTimeout(async () => {
        await fetchAppInfo()
        updatePreview()
      }, 1000)
    })

    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) {
        closeEventSource()
        return
      }

      streamCompleted = true
      closeEventSource()
      handleError(new Error("SSE connection error"), aiMessageIndex)
    }
  } catch (error) {
    console.error("Failed to create EventSource:", error)
    closeEventSource()
    handleError(error, aiMessageIndex)
  }
}

const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error("Code generation failed:", error)
  messages.value[aiMessageIndex].content =
    "Sorry, something went wrong while generating. Please try again."
  messages.value[aiMessageIndex].loading = false
  message.error("Generation failed. Please try again.")
  isGenerating.value = false
}

const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    const newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    previewUrl.value = newPreviewUrl
    previewReady.value = true
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const deployApp = async () => {
  if (!appId.value) {
    message.error("App ID is missing")
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success("Deployed successfully")
    } else {
      message.error("Deployment failed: " + res.data.message)
    }
  } catch (error) {
    console.error("Deployment failed:", error)
    message.error("Deployment failed. Please try again.")
  } finally {
    deploying.value = false
  }
}

const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, "_blank")
  }
}

const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, "_blank")
  }
}

const onIframeLoad = () => {
  previewReady.value = true
}

const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success("Deleted successfully")
      appDetailVisible.value = false
      router.push("/")
    } else {
      message.error("Delete failed: " + res.data.message)
    }
  } catch (error) {
    console.error("Delete failed:", error)
    message.error("Delete failed")
  }
}

const downloading = ref(false)

const downloadCode = async () => {
  if (!appId.value) {
    message.error("App ID is missing")
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ""
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: "GET",
      credentials: "include",
    })
    if (!response.ok) {
      throw new Error(`Download failed: ${response.status}`)
    }
    const contentDisposition = response.headers.get("Content-Disposition")
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement("a")
    link.href = downloadUrl
    link.download = fileName
    link.click()

    URL.revokeObjectURL(downloadUrl)
    message.success("Code downloaded successfully")
  } catch (error) {
    console.error("Download failed:", error)
    message.error("Download failed, please try again")
  } finally {
    downloading.value = false
  }
}

onMounted(() => {
  fetchAppInfo()
})

onUnmounted(() => {
  currentEventSource.value?.close()
  currentEventSource.value = null
})
</script>

<style scoped>
#appChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: #fdfdfd;
}

/* Header */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.header-right {
  display: flex;
  gap: 12px;
}

/* Main layout */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 8px;
  overflow: hidden;
}

/* Chat column */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.messages-container {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 12px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 8px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
}

.user-message .message-content {
  background: #1890ff;
  color: white;
}

.ai-message .message-content {
  background: #f5f5f5;
  color: #1a1a1a;
  padding: 8px 12px;
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 16px;
}

/* Input */
.input-container {
  padding: 16px;
  background: white;
}

.input-wrapper {
  position: relative;
}

.input-wrapper .ant-input {
  padding-right: 50px;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

/* Preview */
.preview-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.preview-loading p {
  margin-top: 16px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .preview-section {
    flex: none;
    height: 50vh;
  }
}

@media (max-width: 768px) {
  .header-bar {
    padding: 12px 16px;
  }

  .app-name {
    font-size: 16px;
  }

  .main-content {
    padding: 8px;
    gap: 8px;
  }

  .message-content {
    max-width: 85%;
  }
}
</style>
