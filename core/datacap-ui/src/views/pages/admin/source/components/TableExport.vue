<template>
  <ShadcnModal v-model="visible"
               width="40%"
               :title="title"
               @on-close="onCancel">
    <div class="px-4 py-4">
      <ShadcnSkeleton v-if="loading" animation/>

      <ShadcnForm v-else-if="!loading && formState" v-model="formState" @on-submit="onSubmit">
        <ShadcnFormItem name="format" :label="$t('source.common.exportDataFormat')">
          <ShadcnToggleGroup v-model="formState.format" name="format">
            <ShadcnToggle v-for="item in formats" :value="item.name">{{ item.name }}</ShadcnToggle>
          </ShadcnToggleGroup>
        </ShadcnFormItem>

        <ShadcnRow>
          <ShadcnCol span="6">
            <ShadcnFormItem name="count" :label="$t('source.common.exportDataCount')">
              <ShadcnNumber v-model="formState.count" name="count"/>
            </ShadcnFormItem>
          </ShadcnCol>
        </ShadcnRow>

        <ShadcnFormItem name="path" :label="$t('source.common.downloadPath')">
          <div class="flex items-center space-x-1">
            <ShadcnInput v-model="formState.path" disabled="" name="path"/>
            <ShadcnButton :disabled="!formState.path" @click="onDownload()">
              {{ $t('source.common.downloadFile') }}
            </ShadcnButton>
          </div>
        </ShadcnFormItem>

        <ShadcnSpace>
          <ShadcnButton type="default" @click="onCancel">{{ $t('common.cancel') }}</ShadcnButton>

          <ShadcnButton submit :loading="submitting" :disabled="submitting">
            {{ $t('source.common.generateData') }}
          </ShadcnButton>
        </ShadcnSpace>
      </ShadcnForm>
    </div>
  </ShadcnModal>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import PluginService from '@/services/plugin'
import MetadataService from '@/services/metadata'

export default defineComponent({
  name: 'TableExport',
  computed: {
    visible: {
      get(): boolean
      {
        return this.isVisible
      },
      set(value: boolean)
      {
        this.$emit('close', value)
      }
    }
  },
  props: {
    isVisible: {
      type: Boolean
    }
  },
  data()
  {
    return {
      loading: false,
      submitting: false,
      title: null as string | null,
      formats: [],
      formState: {
        format: 'JsonConvert',
        count: 5000,
        path: null
      }
    }
  },
  created()
  {
    const table = this.$route.params?.table
    if (table) {
      this.title = this.$t('source.common.exportDataTable').replace('$VALUE', table)

      this.loading = true
      PluginService.getPlugins()
                   .then(response => {
                     if (response.status && response.data) {
                       this.formats = response.data.filter(item => item.type === 'CONVERT')
                     }
                   })
                   .finally(() => this.loading = false)
    }
  },
  methods: {
    onSubmit()
    {
      const code = this.$route.params?.source
      const database = this.$route.params?.database
      const table = this.$route.params?.table

      if (code && database && table) {
        this.submitting = true
        MetadataService.exportData(code, database, table, this.formState)
                       .then(response => {
                         if (response.status) {
                           this.formState.path = response.data
                         }
                         else {
                           this.$Message.error({
                             content: response.message,
                             showIcon: true
                           })
                         }
                       })
                       .finally(() => this.submitting = false)
      }
    },
    onDownload()
    {
      if (this.formState) {
        window.open(this.formState.path, '_target')
      }
    },
    onCancel()
    {
      this.visible = false
    }
  }
})
</script>
