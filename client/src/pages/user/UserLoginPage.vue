<script setup lang="ts">
import { reactive } from "vue"
import { useRouter } from "vue-router"
import { useLoginUserStore } from "@/stores/loginUser.ts"
import { message } from "ant-design-vue"
import { userLogin } from "@/api/userController.ts"

interface LoginForm {
  userAccount: string
  userPassword: string
}

const formState = reactive<API.UserLoginRequest>({
  userAccount: "",
  userPassword: "",
})

const router = useRouter()
const loginUserStore = useLoginUserStore()

const handleSubmit = async (values: LoginForm) => {
  const res = await userLogin(values)
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success("Login successful")

    await router.push({
      path: "/",
      replace: true,
    })
  } else {
    message.error("Login failed，" + res.data.message)
  }
}
</script>

<template>
  <div id="userLoginPage">
    <h2 class="title">AI Web Generator - User Login</h2>
    <div class="desc">Create a website only with prompts</div>
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item
        name="userAccount"
        :rules="[{ required: true, message: 'Please enter user account' }]"
      >
        <a-input v-model:value="formState.userAccount" placeholder="Please enter user account" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: 'Please enter password' },
          { min: 8, message: 'Password must be at least 8 characters' },
        ]"
      >
        <a-input-password
          v-model:value="formState.userPassword"
          placeholder="Please enter password"
        />
      </a-form-item>
      <div class="tips">
        No account?
        <RouterLink to="/user/register">Register</RouterLink>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">Login</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<style>
#userLoginPage {
  max-width: 360px;
  margin: 0 auto;
}

.title {
  text-align: center;
  margin-bottom: 16px;
}

.desc {
  text-align: center;
  color: #bbb;
  margin-bottom: 16px;
}

.tips {
  margin-bottom: 16px;
  color: #bbb;
  font-size: 13px;
  text-align: right;
}
</style>
