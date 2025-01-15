<template>
  <ShadcnModal v-model="visible"
               width="40%"
               :title="title"
               @on-close="onCancel">
    <ShadcnSpace wrap>
      <ShadcnAlert type="error" :title="$t('source.tip.dropColumn1')"/>
      <ShadcnAlert type="error" :title="$t('source.tip.dropColumn2')"/>
      <ShadcnAlert type="error" :title="$t('source.tip.dropColumn3')"/>
      <ShadcnAlert type="error" :title="$t('source.tip.dropColumn4')"/>
      <ShadcnAlert type="error" :title="$t('source.tip.dropColumn5')"/>
    </ShadcnSpace>

    <div class="relative mt-2">
      <ShadcnSpin v-model="loading"/>

      <AceEditor v-if="formState.statement" :value="formState.statement" :read-only="true"/>
    </div>

    <template #footer>
      <ShadcnSpace>
        <ShadcnButton type="default" @click="onCancel">
          {{ $t('common.cancel') }}
        </ShadcnButton>

        <ShadcnButton type="error"
                      :loading="submitting"
                      :disabled="submitting"
                      @click="onSubmit(false)">
          {{ $t('source.common.dropColumn') }}
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
  name: 'ColumnDrop',
  components: { AceEditor },
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
    },
    column: {
      type: String
    }
  },
  data()
  {
    return {
      loading: false,
      submitting: false,
      title: null as string | null,
      formState: null as any
    }
  },
  created()
  {
    this.formState = {
      statement: null
    }

    if (this.column) {
      this.title = this.$t('source.common.dropColumnInfo').replace('$VALUE', this.column)

      this.onSubmit(true)
    }
  },
  methods: {
    onSubmit(preview: boolean)
    {
      const code = this.$route.params?.source
      const database = this.$route.params?.database
      const table = this.$route.params?.table

      if (code && database && table) {
        if (preview) {
          this.loading = true
        }
        else {
          this.submitting = true
        }

        MetadataService.dropColumn(code, database, table, { preview, columns: [{ name: this.column }] })
                       .then(response => {
                         if (response.status) {
                           if (preview) {
                             this.formState.statement = response.data.content
                           }
                           else {
                             this.$Message.success({
                               content: this.$t('source.tip.dropColumnSuccess').replace('$VALUE', String(this.column)),
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
    onCancel()
    {
      this.visible = false
    }
  }
})
</script>