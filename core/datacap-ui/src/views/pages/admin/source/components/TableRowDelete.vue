<template>
  <ShadcnModal v-model="visible"
               height="410"
               width="40%"
               :title="$t('source.common.previewDML')"
               @on-close="onCancel">
    <div class="relative h-full">
      <ShadcnSkeleton v-if="loading" animation/>

      <ShadcnCodeEditor v-else-if="contentDML" v-model="contentDML"
                        :config="{
                        language: 'sql',
                        readOnly: true,
                        minimap: {
                          enabled: false
                        }
                      }">
      </ShadcnCodeEditor>
    </div>

    <template #footer>
      <ShadcnSpace>
        <ShadcnButton type="default" @click="onCancel">
          {{ $t('common.cancel') }}
        </ShadcnButton>

        <ShadcnButton type="danger" :disabled="submitting" :loading="submitting" @click="onSubmit">
          {{ $t('source.common.deleteRows') }}
        </ShadcnButton>
      </ShadcnSpace>
    </template>
  </ShadcnModal>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import MetadataService from '@/services/metadata'
import AceEditor from '@/views/components/editor/AceEditor.vue'

export default defineComponent({
  name: 'TableRowDelete',
  components: { AceEditor },
  props: {
    isVisible: {
      type: Boolean
    },
    columns: {
      type: Array<any>
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

        const columns = this.columns.map(item => {
          return {
            original: item
          }
        })

        const configure = {
          preview: preview,
          columns: columns
        }
        MetadataService.deleteData(code, database, table, configure)
                       .then(response => {
                         if (response.status && response.data && response.data.isSuccessful) {
                           if (preview) {
                             this.contentDML = response.data.content
                           }
                           else {
                             this.$Message.success({
                               content: this.$t('source.tip.deleteSuccess'),
                               showIcon: true
                             })

                             this.onCancel()
                           }
                         }
                         else {
                           this.$Message.error({
                             content: response.message,
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
