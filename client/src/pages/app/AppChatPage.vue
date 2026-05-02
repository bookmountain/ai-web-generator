<template>
  <div id="appChatPage">
    <!-- Top bar -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || "Web App Generator" }}</h1>
      </div>
      <div class="header-right">
        <a-button type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          App Details
        </a-button>
        <a-button type="primary" @click="deployApp" :loading="deploying">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          Deploy
        </a-button>
      </div>
    </div>

    <!-- Main content area -->
    <div class="main-content">
      <!-- Left chat area -->
      <div class="chat-section">
        <!-- Messages area -->
        <div class="messages-container" ref="messagesContainer">
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
                  <span>AI is thinking...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- User message input -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="You can't chat under someone else's work" placement="top">
              <a-textarea
                v-model:value="userInput"
                placeholder="Describe the website you want to generate in as much detail as possible"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
              v-else
              v-model:value="userInput"
              placeholder="Describe the website you want to generate in as much detail as possible"
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

      <!-- Right-side preview area -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>Generated website preview</h3>
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
            <p>The generated website will appear here after generation is complete</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>Generating website...</p>
          </div>
          <iframe
            v-else
            :src="previewUrl"
            class="preview-iframe"
            @load="onIframeLoad"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- App details modal -->
    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <!-- Deployment success modal -->
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

// App information
const appInfo = ref<API.AppVO>()
const appId = ref<string>()

// Chat-related
interface Message {
  type: "user" | "ai"
  content: string
  loading?: boolean
}

const messages = ref<Message[]>([])
const userInput = ref("")
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()
const hasInitialConversation = ref(false) // Track whether the initial conversation has already happened

// Preview-related
const previewUrl = ref("")
const previewReady = ref(false)

// Deployment-related
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref("")

// Permission-related
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === "admin"
})

// App details-related
const appDetailVisible = ref(false)

// Show app details
const showAppDetail = () => {
  appDetailVisible.value = true
}

// Fetch app info
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error("App ID does not exist")
    router.push("/")
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // Check whether the view=1 parameter exists; if so, do not auto-send the initial prompt
      const isViewMode = route.query.view === "1"

      // Auto-send the initial prompt unless in view mode or the initial conversation has already happened
      if (appInfo.value.initPrompt && !isViewMode && !hasInitialConversation.value) {
        hasInitialConversation.value = true
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error("Failed to fetch app info")
      router.push("/")
    }
  } catch (error) {
    console.error("Failed to fetch app info:", error)
    message.error("Failed to fetch app info")
    router.push("/")
  }
}

// Send the initial message
const sendInitialMessage = async (prompt: string) => {
  // Add user message
  messages.value.push({
    type: "user",
    content: prompt,
  })

  // Add AI message placeholder
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: "ai",
    content: "",
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // Start generation
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// Send message
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  const message = userInput.value.trim()
  userInput.value = ""

  // Add user message
  messages.value.push({
    type: "user",
    content: message,
  })

  // Add AI message placeholder
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: "ai",
    content: "",
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // Start generation
  isGenerating.value = true
  await generateCode(message, aiMessageIndex)
}

// Generate code using EventSource for streaming responses
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false

  try {
    // Get the baseURL configured in axios
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // Build URL parameters
    const params = new URLSearchParams({
      appId: appId.value || "",
      message: userMessage,
    })

    const url = `${baseURL}/app/chat/gen/code?${params}`

    // Create EventSource connection
    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    let fullContent = ""

    // Handle received messages
    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        // Parse the JSON-wrapped data
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        // Append content
        if (content !== undefined && content !== null) {
          fullContent += content
          messages.value[aiMessageIndex].content = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (error) {
        console.error("Failed to parse message:", error)
        handleError(error, aiMessageIndex)
      }
    }

    // Handle the done event
    eventSource.addEventListener("done", function () {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      eventSource?.close()

      // Delay preview updates to ensure the backend has finished processing
      setTimeout(async () => {
        await fetchAppInfo()
        updatePreview()
      }, 1000)
    })

    // Handle errors
    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // Check whether this is a normal connection close
      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()

        setTimeout(async () => {
          await fetchAppInfo()
          updatePreview()
        }, 1000)
      } else {
        handleError(new Error("SSE connection error"), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error("Failed to create EventSource:", error)
    handleError(error, aiMessageIndex)
  }
}

// Error handling function
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error("Code generation failed:", error)
  messages.value[aiMessageIndex].content = "Sorry, an error occurred during generation. Please try again."
  messages.value[aiMessageIndex].loading = false
  message.error("Generation failed, please try again")
  isGenerating.value = false
}

// Update preview
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    previewUrl.value = getStaticPreviewUrl(codeGenType, appId.value)
    previewReady.value = true
  }
}

// Scroll to bottom
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// Deploy app
const deployApp = async () => {
  if (!appId.value) {
    message.error("App ID does not exist")
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
      message.success("Deployment successful")
    } else {
      message.error("Deployment failed: " + res.data.message)
    }
  } catch (error) {
    console.error("Deployment failed:", error)
    message.error("Deployment failed, please try again")
  } finally {
    deploying.value = false
  }
}

// Open preview in a new window
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, "_blank")
  }
}

// Open the deployed site
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, "_blank")
  }
}

// iframe loaded
const onIframeLoad = () => {
  previewReady.value = true
}

// Edit app
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// Delete app
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

// Fetch app info on page load
onMounted(() => {
  fetchAppInfo()
})

// Clean up resources
onUnmounted(() => {
  // EventSource is automatically cleaned up when the component unmounts
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

/* Top bar */
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

/* Main content area */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 8px;
  overflow: hidden;
}

/* Left chat area */
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

/* Input area */
.input-container {
  padding: 16px;
  background: white;
}

.input-wrapper {
  position: relative;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

/* Right preview area */
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

/* Responsive design */
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
