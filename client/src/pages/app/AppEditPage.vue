<template>
  <div id="appEditPage">
    <div class="page-header">
      <h1>Edit App Info</h1>
    </div>

    <div class="edit-container">
      <a-card title="Basic Information" :loading="loading">
        <a-form
          :model="formData"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
          ref="formRef"
        >
          <a-form-item label="App Name" name="appName">
            <a-input
              v-model:value="formData.appName"
              placeholder="Please enter the app name"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            label="App Cover"
            name="cover"
            extra="Supports image URLs; recommended size: 400x300"
          >
            <a-input v-model:value="formData.cover" placeholder="Please enter the cover image URL" />
            <div v-if="formData.cover" class="cover-preview">
              <a-image
                :src="formData.cover"
                :width="200"
                :height="150"
                fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
              />
            </div>
          </a-form-item>

          <a-form-item v-if="isAdmin" label="Priority" name="priority" extra="Set to 99 to mark as a featured app">
            <a-input-number
              v-model:value="formData.priority"
              :min="0"
              :max="99"
              style="width: 200px"
            />
          </a-form-item>

          <a-form-item label="Initial Prompt" name="initPrompt">
            <a-textarea
              v-model:value="formData.initPrompt"
              placeholder="Please enter the initial prompt"
              :rows="4"
              :maxlength="1000"
              show-count
              disabled
            />
            <div class="form-tip">The initial prompt cannot be modified</div>
          </a-form-item>

          <a-form-item label="Generation Type" name="codeGenType">
            <a-input
              :value="formatCodeGenType(formData.codeGenType)"
              placeholder="Generation type"
              disabled
            />
            <div class="form-tip">The generation type cannot be modified</div>
          </a-form-item>

          <a-form-item v-if="formData.deployKey" label="Deployment Key" name="deployKey">
            <a-input v-model:value="formData.deployKey" placeholder="Deployment key" disabled />
            <div class="form-tip">The deployment key cannot be modified</div>
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" :loading="submitting">
                Save Changes
              </a-button>
              <a-button @click="resetForm">Reset</a-button>
              <a-button type="link" @click="goToChat">Open Chat</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <!-- App information -->
      <a-card title="App Information" style="margin-top: 24px">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="App ID">
            {{ appInfo?.id }}
          </a-descriptions-item>
          <a-descriptions-item label="Creator">
            <UserInfo :user="appInfo?.user" size="small" />
          </a-descriptions-item>
          <a-descriptions-item label="Created At">
            {{ formatTime(appInfo?.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="Updated At">
            {{ formatTime(appInfo?.updateTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="Deployed At">
            {{ appInfo?.deployedTime ? formatTime(appInfo.deployedTime) : 'Not deployed' }}
          </a-descriptions-item>
          <a-descriptions-item label="Preview Link">
            <a-button v-if="appInfo?.deployKey" type="link" @click="openPreview" size="small">
              View Preview
            </a-button>
            <span v-else>Not deployed</span>
          </a-descriptions-item>
        </a-descriptions>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { getAppVoById, updateApp, updateAppByAdmin } from '@/api/appController'
import { formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'
import UserInfo from '@/components/UserInfo.vue'
import { getStaticPreviewUrl } from '@/config/env'
import type { FormInstance } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// App Information
const appInfo = ref<API.AppVO>()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

// Form data
const formData = reactive({
  appName: '',
  cover: '',
  priority: 0,
  initPrompt: '',
  codeGenType: '',
  deployKey: '',
})

// Whether the user is an admin
const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// Form validation rules
const rules = {
  appName: [
    { required: true, message: 'Please enter the app name', trigger: 'blur' },
    { min: 1, max: 50, message: 'App name length must be between 1 and 50 characters', trigger: 'blur' },
  ],
  cover: [{ type: 'url', message: 'Please enter a valid URL', trigger: 'blur' }],
  priority: [{ type: 'number', min: 0, max: 99, message: 'Priority must be between 0 and 99', trigger: 'blur' }],
}

// Fetch app information
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('App ID does not exist')
    router.push('/')
    return
  }

  loading.value = true
  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // Check permissions
      if (!isAdmin.value && appInfo.value.userId !== loginUserStore.loginUser.id) {
        message.error('You do not have permission to edit this app')
        router.push('/')
        return
      }

      // Populate form data
      formData.appName = appInfo.value.appName || ''
      formData.cover = appInfo.value.cover || ''
      formData.priority = appInfo.value.priority || 0
      formData.initPrompt = appInfo.value.initPrompt || ''
      formData.codeGenType = appInfo.value.codeGenType || ''
      formData.deployKey = appInfo.value.deployKey || ''
    } else {
      message.error('Failed to fetch app info')
      router.push('/')
    }
  } catch (error) {
    console.error('Failed to fetch app info:', error)
    message.error('Failed to fetch app info')
    router.push('/')
  } finally {
    loading.value = false
  }
}

// Submit the form
const handleSubmit = async () => {
  if (!appInfo.value?.id) return

  submitting.value = true
  try {
    let res
    if (isAdmin.value) {
      // Admins can modify more fields
      res = await updateAppByAdmin({
        id: appInfo.value.id,
        appName: formData.appName,
        cover: formData.cover,
        priority: formData.priority,
      })
    } else {
      // Regular users can only modify the app name
      res = await updateApp({
        id: appInfo.value.id,
        appName: formData.appName,
      })
    }

    if (res.data.code === 0) {
      message.success('Changes saved successfully')
      // Refresh app info
      await fetchAppInfo()
    } else {
      message.error('Update failed: ' + res.data.message)
    }
  } catch (error) {
    console.error('Update failed:', error)
    message.error('Update failed')
  } finally {
    submitting.value = false
  }
}

// Reset form
const resetForm = () => {
  if (appInfo.value) {
    formData.appName = appInfo.value.appName || ''
    formData.cover = appInfo.value.cover || ''
    formData.priority = appInfo.value.priority || 0
  }
  formRef.value?.clearValidate()
}

// Go to the chat page
const goToChat = () => {
  if (appInfo.value?.id) {
    router.push(`/app/chat/${appInfo.value.id}`)
  }
}

// Open preview
const openPreview = () => {
  if (appInfo.value?.codeGenType && appInfo.value?.id) {
    const url = getStaticPreviewUrl(appInfo.value.codeGenType, String(appInfo.value.id))
    window.open(url, '_blank')
  }
}

// Fetch app info on page load
onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped>
#appEditPage {
  padding: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.edit-container {
  border-radius: 8px;
}

.cover-preview {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background: #fafafa;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
