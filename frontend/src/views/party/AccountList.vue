<template>
  <el-card>
    <div class="toolbar">
      <div class="breadcrumb">相关方 > {{ partyName }} > 账户</div>
      <el-button type="default" @click="goBack">返回</el-button>
    </div>

    <div class="toolbar">
      <el-button v-permission="'party:account:create'" type="success" @click="openCreate">
        新增账户
      </el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="accountName" label="户名" />
      <el-table-column prop="accountNo" label="银行账号" />
      <el-table-column prop="bankName" label="开户银行" />
      <el-table-column prop="bankBranch" label="开户支行" />
      <el-table-column label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'party:account:update'" link type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-permission="'party:account:delete'" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑账户' : '新增账户'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="户名" prop="accountName">
          <el-input v-model="form.accountName" />
        </el-form-item>
        <el-form-item label="银行账号" prop="accountNo">
          <el-input v-model="form.accountNo" />
        </el-form-item>
        <el-form-item label="开户银行">
          <el-autocomplete
            v-model="form.bankName"
            :fetch-suggestions="queryBankNames"
            placeholder="如 中国工商银行"
            clearable
            class="full"
          />
        </el-form-item>
        <el-form-item label="开户支行">
          <el-input v-model="form.bankBranch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listAccounts,
  createAccount,
  updateAccount,
  deleteAccount,
  listBankNames,
  type AccountItem,
} from '@/api/party'
import { formatDateTime } from '@/utils/time'

const router = useRouter()
const route = useRoute()

const partyId = route.params.partyId as string
const partyName = route.query.partyName as string

const loading = ref(false)
const list = ref<AccountItem[]>([])

async function load() {
  loading.value = true
  try {
    list.value = await listAccounts(partyId)
  } finally {
    loading.value = false
  }
}

onMounted(load)

function goBack() {
  router.back()
}

const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  accountName: '',
  accountNo: '',
  bankName: '',
  bankBranch: '',
  status: 1,
  remark: '',
})
const rules: FormRules = {
  accountName: [{ required: true, message: '请输入户名', trigger: 'blur' }],
  accountNo: [{ required: true, message: '请输入银行账号', trigger: 'blur' }],
}

// 银行名称补全
const bankNames = ref<string[]>([])
const bankNamesLoaded = ref(false)
async function ensureBankNames() {
  if (bankNamesLoaded.value) return
  bankNames.value = await listBankNames()
  bankNamesLoaded.value = true
}
function queryBankNames(queryString: string, cb: (suggestions: { value: string }[]) => void) {
  const q = (queryString || '').toLowerCase()
  const matched = bankNames.value.filter((t) => !q || t.toLowerCase().includes(q))
  cb(matched.map((t) => ({ value: t })))
}

function resetForm() {
  form.accountName = ''
  form.accountNo = ''
  form.bankName = ''
  form.bankBranch = ''
  form.status = 1
  form.remark = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  ensureBankNames()
  dialogVisible.value = true
}

function openEdit(row: AccountItem) {
  editingId.value = row.id
  form.accountName = row.accountName
  form.accountNo = row.accountNo
  form.bankName = row.bankName ?? ''
  form.bankBranch = row.bankBranch ?? ''
  form.status = row.status
  form.remark = row.remark ?? ''
  ensureBankNames()
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (editingId.value) {
      await updateAccount(editingId.value, { ...form })
      ElMessage.success('保存成功')
    } else {
      await createAccount(partyId, { ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  })
}

async function handleDelete(row: AccountItem) {
  await ElMessageBox.confirm(`确认删除账户「${row.accountName}」？`, '提示', { type: 'warning' })
  await deleteAccount(row.id)
  ElMessage.success('删除成功')
  await load()
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.breadcrumb {
  flex: 1;
  line-height: 32px;
  font-size: 14px;
  color: #606266;
}
.full {
  width: 100%;
}
</style>
