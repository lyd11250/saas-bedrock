<template>
  <el-card>
    <div class="toolbar">
      <div class="breadcrumb">相关方 > {{ partyName }} > 资质</div>
      <el-button type="default" @click="goBack">返回</el-button>
    </div>

    <div class="toolbar">
      <el-button v-permission="'party:qualification:create'" type="success" @click="openCreate">
        新增资质
      </el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="qualType" label="资质类型" />
      <el-table-column prop="qualName" label="资质名称" />
      <el-table-column prop="qualLevel" label="资质等级" />
      <el-table-column prop="qualNo" label="证书编号" />
      <el-table-column prop="issuingAuthority" label="发证机关" />
      <el-table-column prop="issueDate" label="发证日期" />
      <el-table-column prop="expiryDate" label="有效期至" />
      <el-table-column label="附件">
        <template #default="{ row }">
          <div v-if="row.fileName" class="file-cell">
            <el-link type="primary" @click="previewFile(row)">{{ row.fileName }}</el-link>
          </div>
          <span v-else style="color: #909399">未上传</span>
        </template>
      </el-table-column>
      <el-table-column label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '有效' : '失效' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'party:qualification:update'" link type="warning" @click="openUpload(row)">
            上传附件
          </el-button>
          <el-button v-permission="'party:qualification:update'" link type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-permission="'party:qualification:delete'" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑资质' : '新增资质'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="资质类型" prop="qualType">
          <el-autocomplete
            v-model="form.qualType"
            :fetch-suggestions="queryQualTypes"
            placeholder="特种作业操作证 / 驾驶证…"
            clearable
            class="full"
          />
        </el-form-item>
        <el-form-item label="资质名称" prop="qualName">
          <el-input v-model="form.qualName" />
        </el-form-item>
        <el-form-item label="资质等级">
          <el-autocomplete
            v-model="form.qualLevel"
            :fetch-suggestions="queryQualLevels"
            placeholder="C1 / 壹级…"
            clearable
            class="full"
          />
        </el-form-item>
        <el-form-item label="证书编号">
          <el-input v-model="form.qualNo" />
        </el-form-item>
        <el-form-item label="发证机关">
          <el-input v-model="form.issuingAuthority" />
        </el-form-item>
        <el-form-item label="发证日期">
          <el-date-picker
            v-model="form.issueDate"
            type="date"
            placeholder="选择发证日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="full"
          />
        </el-form-item>
        <el-form-item label="有效期至">
          <el-date-picker
            v-model="form.expiryDate"
            type="date"
            placeholder="长期有效"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="full"
          />
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

    <input ref="fileInput" type="file" style="display: none" @change="handleFileSelect" />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listQualifications,
  createQualification,
  updateQualification,
  deleteQualification,
  listQualTypes,
  listQualLevels,
  uploadQualificationFile,
  type QualificationItem,
} from '@/api/party'

const router = useRouter()
const route = useRoute()

const partyId = route.params.partyId as string
const partyName = route.query.partyName as string

const loading = ref(false)
const list = ref<QualificationItem[]>([])

async function load() {
  loading.value = true
  try {
    list.value = await listQualifications(partyId)
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
  qualType: '',
  qualName: '',
  qualLevel: '',
  qualNo: '',
  issuingAuthority: '',
  issueDate: '' as string | null,
  expiryDate: '' as string | null,
  status: 1,
  remark: '',
})
const rules: FormRules = {
  qualType: [{ required: true, message: '请选择资质类型', trigger: 'blur' }],
  qualName: [{ required: true, message: '请输入资质名称', trigger: 'blur' }],
}

// 资质类型补全
const qualTypes = ref<string[]>([])
const qualTypesLoaded = ref(false)
async function ensureQualTypes() {
  if (qualTypesLoaded.value) return
  qualTypes.value = await listQualTypes()
  qualTypesLoaded.value = true
}
function queryQualTypes(queryString: string, cb: (suggestions: { value: string }[]) => void) {
  const q = (queryString || '').toLowerCase()
  const matched = qualTypes.value.filter((t) => !q || t.toLowerCase().includes(q))
  cb(matched.map((t) => ({ value: t })))
}

// 资质等级补全
const qualLevels = ref<string[]>([])
const qualLevelsLoaded = ref(false)
async function ensureQualLevels() {
  if (qualLevelsLoaded.value) return
  qualLevels.value = await listQualLevels()
  qualLevelsLoaded.value = true
}
function queryQualLevels(queryString: string, cb: (suggestions: { value: string }[]) => void) {
  const q = (queryString || '').toLowerCase()
  const matched = qualLevels.value.filter((t) => !q || t.toLowerCase().includes(q))
  cb(matched.map((t) => ({ value: t })))
}

function resetForm() {
  form.qualType = ''
  form.qualName = ''
  form.qualLevel = ''
  form.qualNo = ''
  form.issuingAuthority = ''
  form.issueDate = ''
  form.expiryDate = ''
  form.status = 1
  form.remark = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  ensureQualTypes()
  ensureQualLevels()
  dialogVisible.value = true
}

function openEdit(row: QualificationItem) {
  editingId.value = row.id
  form.qualType = row.qualType
  form.qualName = row.qualName
  form.qualLevel = row.qualLevel ?? ''
  form.qualNo = row.qualNo ?? ''
  form.issuingAuthority = row.issuingAuthority ?? ''
  form.issueDate = row.issueDate ?? ''
  form.expiryDate = row.expiryDate ?? ''
  form.status = row.status
  form.remark = row.remark ?? ''
  ensureQualTypes()
  ensureQualLevels()
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    const payload = {
      ...form,
      issueDate: form.issueDate || undefined,
      expiryDate: form.expiryDate || undefined,
    }
    if (editingId.value) {
      await updateQualification(editingId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await createQualification(partyId, payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  })
}

async function handleDelete(row: QualificationItem) {
  await ElMessageBox.confirm(`确认删除资质「${row.qualName}」？`, '提示', { type: 'warning' })
  await deleteQualification(row.id)
  ElMessage.success('删除成功')
  await load()
}

// 文件上传
const fileInput = ref<HTMLInputElement>()
let uploadingQualId: string | null = null

function openUpload(row: QualificationItem) {
  uploadingQualId = row.id
  fileInput.value?.click()
}

async function handleFileSelect(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files?.length || !uploadingQualId) return

  const file = files[0]
  try {
    await uploadQualificationFile(uploadingQualId, file)
    ElMessage.success('附件上传成功')
    await load()
  } catch {
    ElMessage.error('附件上传失败')
  } finally {
    uploadingQualId = null
    if (fileInput.value) fileInput.value.value = ''
  }
}

// 文件预览
async function previewFile(row: QualificationItem) {
  if (!row.fileId) return
  try {
    const url = `/api/v1/party/qualifications/${row.id}/file`
    if (row.fileContentType?.startsWith('image/')) {
      window.open(url, '_blank')
    } else {
      const a = document.createElement('a')
      a.href = url
      a.download = row.fileName || 'file'
      a.click()
    }
  } catch {
    ElMessage.error('文件加载失败')
  }
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
.file-cell {
  cursor: pointer;
}
.full {
  width: 100%;
}
</style>
