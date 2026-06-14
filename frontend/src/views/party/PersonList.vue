<template>
  <el-card>
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="姓名"
        clearable
        class="search"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button v-permission="'party:person:create'" type="success" @click="openCreate"
        >新建人员</el-button
      >
    </div>

    <el-table
      v-loading="loading"
      :data="list"
      border
      :header-cell-style="{ textAlign: 'center' }"
      :cell-style="{ textAlign: 'center' }"
      stripe
    >
      <el-table-column prop="name" label="姓名" />
      <el-table-column label="性别">
        <template #default="{ row }">{{ genderText(row.gender) }}</template>
      </el-table-column>
      <el-table-column prop="idCard" label="身份证号" />
      <el-table-column prop="contact" label="联系方式" />
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
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button v-permission="'party:person:update'" link type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button
            v-permission="'party:person:delete'"
            link
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
          <el-button v-permission="'party:account:list'" link type="warning" @click="openAccounts(row)">
            账户
          </el-button>
          <el-button v-permission="'party:qualification:list'" link type="warning" @click="openQualifications(row)">
            资质
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.page"
      @current-change="
        (p: number) => {
          query.page = p
          load()
        }
      "
    />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑人员' : '新建人员'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="0">未知</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.contact" />
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createPerson,
  deletePerson,
  pagePersons,
  updatePerson,
  type PersonItem,
} from '@/api/party'
import { formatDateTime } from '@/utils/time'

const router = useRouter()

const loading = ref(false)
const list = ref<PersonItem[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

function genderText(g: number) {
  return g === 1 ? '男' : g === 2 ? '女' : '未知'
}

async function load() {
  loading.value = true
  try {
    const res = await pagePersons(query)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function reload() {
  query.page = 1
  load()
}

onMounted(load)

const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  gender: 0,
  idCard: '',
  contact: '',
  status: 1,
  remark: '',
})
const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  idCard: [
    {
      pattern: /^$|^\d{15}$|^\d{17}[\dXx]$/,
      message: '身份证号格式不正确',
      trigger: 'blur',
    },
  ],
}

function resetForm() {
  form.name = ''
  form.gender = 0
  form.idCard = ''
  form.contact = ''
  form.status = 1
  form.remark = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: PersonItem) {
  editingId.value = row.id
  form.name = row.name
  form.gender = row.gender ?? 0
  form.idCard = row.idCard ?? ''
  form.contact = row.contact ?? ''
  form.status = row.status
  form.remark = row.remark ?? ''
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (editingId.value) {
      await updatePerson(editingId.value, { ...form })
      ElMessage.success('保存成功')
    } else {
      await createPerson({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  })
}

async function handleDelete(row: PersonItem) {
  await ElMessageBox.confirm(`确认删除人员「${row.name}」？`, '提示', { type: 'warning' })
  await deletePerson(row.id)
  ElMessage.success('删除成功')
  await load()
}

function openAccounts(row: PersonItem) {
  router.push({ name: 'party-accounts', params: { partyId: row.id }, query: { partyName: row.name } })
}

function openQualifications(row: PersonItem) {
  router.push({ name: 'party-qualifications', params: { partyId: row.id }, query: { partyName: row.name } })
}</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.search {
  width: 220px;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
