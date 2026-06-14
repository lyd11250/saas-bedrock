<template>
  <el-card>
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="单位名称"
        clearable
        class="search"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button v-permission="'party:organization:create'" type="success" @click="openCreate"
        >新建单位</el-button
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
      <el-table-column prop="name" label="单位名称" />
      <el-table-column prop="orgType" label="组织类型" />
      <el-table-column prop="taxNo" label="统一社会信用代码" />
      <el-table-column prop="legalPerson" label="法定代表人" />
      <el-table-column label="成立日期">
        <template #default="{ row }">{{ row.establishedDate || '' }}</template>
      </el-table-column>
      <el-table-column label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button
            v-permission="'party:organization:update'"
            link
            type="primary"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            v-permission="'party:organization:delete'"
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑单位' : '新建单位'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <el-form-item label="单位名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="组织类型">
          <el-autocomplete
            v-model="form.orgType"
            :fetch-suggestions="queryTypes"
            placeholder="如 企业 / 政府机构 / 事业单位"
            clearable
            class="full"
          />
        </el-form-item>
        <el-form-item label="统一社会信用代码">
          <el-input v-model="form.taxNo" />
        </el-form-item>
        <el-form-item label="法定代表人">
          <el-input v-model="form.legalPerson" />
        </el-form-item>
        <el-form-item label="注册资本">
          <el-input
            v-model="form.registeredCapital"
            placeholder="如 500 万元人民币（仅企业适用）"
          />
        </el-form-item>
        <el-form-item label="成立日期">
          <el-date-picker
            v-model="form.establishedDate"
            type="date"
            placeholder="选择成立日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="full"
          />
        </el-form-item>
        <el-form-item label="住所">
          <el-input v-model="form.regAddress" />
        </el-form-item>
        <el-form-item label="经营范围">
          <el-input v-model="form.businessScope" type="textarea" :rows="3" />
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
  createOrganization,
  deleteOrganization,
  listOrganizationTypes,
  pageOrganizations,
  updateOrganization,
  type OrganizationItem,
} from '@/api/party'

const router = useRouter()

const loading = ref(false)
const list = ref<OrganizationItem[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

// 历史组织类型（去重），供输入补全。
const types = ref<string[]>([])
const typesLoaded = ref(false)
async function ensureTypes() {
  if (typesLoaded.value) return
  types.value = await listOrganizationTypes()
  typesLoaded.value = true
}
function queryTypes(queryString: string, cb: (suggestions: { value: string }[]) => void) {
  const q = (queryString || '').toLowerCase()
  const matched = types.value.filter((t) => !q || t.toLowerCase().includes(q))
  cb(matched.map((t) => ({ value: t })))
}

async function load() {
  loading.value = true
  try {
    const res = await pageOrganizations(query)
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
  orgType: '',
  taxNo: '',
  registeredCapital: '',
  establishedDate: '' as string | null,
  legalPerson: '',
  regAddress: '',
  businessScope: '',
  status: 1,
  remark: '',
})
const rules: FormRules = {
  name: [{ required: true, message: '请输入单位名称', trigger: 'blur' }],
}

function resetForm() {
  form.name = ''
  form.orgType = ''
  form.taxNo = ''
  form.registeredCapital = ''
  form.establishedDate = ''
  form.legalPerson = ''
  form.regAddress = ''
  form.businessScope = ''
  form.status = 1
  form.remark = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  ensureTypes()
  dialogVisible.value = true
}

function openEdit(row: OrganizationItem) {
  editingId.value = row.id
  form.name = row.name
  form.orgType = row.orgType ?? ''
  form.taxNo = row.taxNo ?? ''
  form.registeredCapital = row.registeredCapital ?? ''
  form.establishedDate = row.establishedDate ?? ''
  form.legalPerson = row.legalPerson ?? ''
  form.regAddress = row.regAddress ?? ''
  form.businessScope = row.businessScope ?? ''
  form.status = row.status
  form.remark = row.remark ?? ''
  ensureTypes()
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    const payload = { ...form, establishedDate: form.establishedDate || undefined }
    if (editingId.value) {
      await updateOrganization(editingId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await createOrganization(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    typesLoaded.value = false
    await load()
  })
}

async function handleDelete(row: OrganizationItem) {
  await ElMessageBox.confirm(`确认删除单位「${row.name}」？`, '提示', { type: 'warning' })
  await deleteOrganization(row.id)
  ElMessage.success('删除成功')
  await load()
}

function openAccounts(row: OrganizationItem) {
  router.push({ name: 'party-accounts', params: { partyId: row.id }, query: { partyName: row.name } })
}

function openQualifications(row: OrganizationItem) {
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
.full {
  width: 100%;
}
</style>
