<template>
  <ShadcnModal v-model="visible"
               height="410"
               width="40%"
               :title="$t('source.common.previewDML')"
               @on-close="onCancel">
    <div class="relative h-full">
      <ShadcnSpin v-model="loading" fixed/>

      <AceEditor v-if="!loading && contentDML" :value="contentDML" :read-only="true"/>
    </div>

    <template #footer>
      <ShadcnSpace>
        <ShadcnButton type="default" @click="onCancel">
          {{ $t('common.cancel') }}
        </ShadcnButton>

        <ShadcnButton type="primary" :disabled="submitting" :loading="submitting" @click="onSubmit">
          {{ $t('common.submit') }}
        </ShadcnButton>
      </ShadcnSpace>
    </template>
  </ShadcnModal>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import AceEditor from '@/views/components/editor/AceEditor.vue'
import MetadataService from '@/services/metadata.ts'

export default defineComponent({
  name: 'TableCellInfo',
  components: { AceEditor },
  props: {
    isVisible: {
      type: Boolean
    },
    columns: {
      type: Array<any>
    },
    isUpdate: {
      type: Boolean
    }
  },
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
  data()
  {
    return {
      loading: false,
      submitting: false,
      contentDML: null as string | null,
      configure: null as any
    }
  },
  created()
  {
    this.onChange(true)
  },
  methods: {
    onChange(preview: boolean)
    {
      const code = this.$route?.params.source
      const database = this.$route?.params.database
      const table = this.$route?.params.table

      if (code && database && table) {
        if (preview) {
          this.loading = true
        }
        else {
          this.submitting = true
        }

        const configure = {
          preview: preview,
          rows: this.columns
        }

        const apiCall = this.isUpdate
            ? MetadataService.updateData(code, database, table, configure)
            : MetadataService.insertData(code, database, table, configure)

        apiCall.then(response => {
          if (response.status && response.data && response.data.isSuccessful) {
            if (preview) {
              this.contentDML = response.data.content
            }
            else {
              this.$Message.success({
                content: this.$t('source.tip.updateSuccess'),
                showIcon: true
              })

              this.onCancel()
            }
          }
          else {
            this.$Message.error({
              content: response.data.message,
              showIcon: true
            })
          }
        })
               .finally(() => {
                 if (preview) {
                   this.loading = false
                 }
                 else {
                   this.submitting = false
                 }
               })
      }
    },
    onSubmit()
    {
      this.onChange(false)
    },
    onCancel()
    {
      this.visible = false
    }
  }
})
</script>
