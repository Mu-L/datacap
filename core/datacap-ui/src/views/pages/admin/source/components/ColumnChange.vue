<template>
  <ShadcnModal v-model="visible"
               height="60%"
               width="40%"
               :title="$t('source.common.newColumn')"
               @on-close="onCancel">
    <ShadcnSkeleton v-if="loading" animation/>

    <ShadcnForm v-else-if="formState" v-model="formState" @on-submit="onSubmit">
      <ShadcnRow gutter="16">
        <ShadcnCol v-for="(_item, index) in formState.columns" span="12">
          <ShadcnRow gutter="16">
            <ShadcnCol span="6">
              <ShadcnFormItem :name="`columns[${index}].name`"
                              :label="`${$t('source.common.columnName')} ${index + 1}`"
                              :rules="[
                                  { required: true, message: $t('source.validator.columnName.required') },
                                  { pattern: /^[A-Za-z][A-Za-z0-9_-]*$/, message: $t('source.validator.columnName.pattern') }
                              ]">
                <ShadcnInput v-model="formState.columns[index].name" :placeholder="$t('source.placeholder.columnName')" :name="`columns[${index}].name`"/>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="6">
              <ShadcnFormItem :name="`columns[${index}].type`"
                              :label="$t('source.common.columnType')"
                              :rules="[{ required: true, message: $t('source.validator.columnType.required') }]">
                <ShadcnSelect v-model="formState.columns[index].type"
                              :placeholder="$t('source.placeholder.columnType')"
                              :loading="loading"
                              :name="`columns[${index}].type`">
                  <template #options>
                    <ShadcnSelectOption v-for="dataType in dataTypes"
                                        :key="dataType"
                                        :value="dataType"
                                        :label="dataType">
                    </ShadcnSelectOption>
                  </template>
                </ShadcnSelect>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="6">
              <ShadcnFormItem :name="`columns[${index}].length`"
                              :label="$t('source.common.columnLength')"
                              :rules="[
                                  { required: true, message: $t('source.validator.columnLength.required') },
                                  { min: 1, message: $t('source.validator.columnLength.min') }
                              ]">
                <ShadcnNumber v-model="formState.columns[index].length"
                              :placeholder="$t('source.placeholder.columnLength')"
                              :name="`columns[${index}].length`"
                              :min="1"/>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="6">
              <ShadcnFormItem :name="`columns[${index}].defaultValue`" :label="$t('source.common.columnDefaultValue')">
                <ShadcnInput v-model="formState.columns[index].defaultValue" :placeholder="$t('source.placeholder.columnDefaultValue')" :name="`columns[${index}].defaultValue`"/>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="4">
              <ShadcnFormItem :name="`columns[${index}].primaryKey`" :label="$t('source.common.columnPrimaryKey')">
                <ShadcnSwitch v-model="formState.columns[index].primaryKey" :placeholder="$t('source.placeholder.columnPrimaryKey')" :name="`columns[${index}].primaryKey`"/>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="4">
              <ShadcnFormItem :name="`columns[${index}].autoIncrement`" :label="$t('source.common.columnAutoIncrement')">
                <ShadcnSwitch v-model="formState.columns[index].autoIncrement" :placeholder="$t('source.placeholder.columnAutoIncrement')"
                              :name="`columns[${index}].autoIncrement`"/>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="4">
              <ShadcnFormItem :name="`columns[${index}].isNullable`" :label="$t('source.common.columnIsNullable')">
                <ShadcnSwitch v-model="formState.columns[index].isNullable"
                              :placeholder="$t('source.placeholder.columnIsNullable')"
                              true-value="YES"
                              false-value="NO"
                              :name="`columns[${index}].isNullable`"/>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="12">
              <ShadcnFormItem :name="`columns[${index}].comment`" :label="$t('source.common.columnComment')">
                <ShadcnInput v-model="formState.columns[index].comment"
                             type="textarea"
                             :placeholder="$t('source.placeholder.columnComment')"
                             :name="`columns[${index}].comment`"/>
              </ShadcnFormItem>
            </ShadcnCol>
          </ShadcnRow>
        </ShadcnCol>
      </ShadcnRow>

      <ShadcnSpace>
        <ShadcnButton type="default" @click="onCancel">{{ $t('common.cancel') }}</ShadcnButton>

        <ShadcnButton submit :loading="saving" :disabled="saving">
          {{ $t('common.save') }}
        </ShadcnButton>
      </ShadcnSpace>
    </ShadcnForm>
  </ShadcnModal>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import MetadataService from '@/services/metadata'
import HttpUtils from '@/utils/http'

export default defineComponent({
  name: 'ColumnChange',
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
      saving: false,
      dataTypes: [],
      formState: null as any
    }
  },
  created()
  {
    this.handleInitialize()
  },
  methods: {
    handleInitialize()
    {
      const code = this.$route.params.source
      const database = this.$route.params.database
      const table = this.$route.params.table

      this.formState = {
        columns: []
      }

      if (code && database && table) {
        this.loading = true
        HttpUtils.all([MetadataService.getColumn(code, database, table, { columns: [{ name: this.column }] }), MetadataService.getDataTypes(code)])
                 .then(HttpUtils.spread((...responses) => {
                   const [columnResponse, dataTypeResponse] = responses

                   if (columnResponse.status && columnResponse.data && columnResponse.data.isSuccessful) {
                     const primaryKey = columnResponse.data.columns.find(item => item.type_name === 'primary')

                     columnResponse.data.columns
                                   .filter(item => item.type_name === 'column')
                                   .forEach(item => {
                                     this.formState.columns.push({
                                       name: item.object_name,
                                       type: item.object_data_type,
                                       length: item.object_length,
                                       comment: item.object_comment,
                                       defaultValue: item.object_default_value,
                                       primaryKey: primaryKey !== undefined,
                                       autoIncrement: item.extra === 'auto_increment',
                                       isNullable: item.object_nullable
                                     })
                                   })
                   }
                   else {
                     this.$Message.error({
                       content: columnResponse.message,
                       showIcon: true
                     })
                   }

                   if (dataTypeResponse.status && dataTypeResponse.data && dataTypeResponse.data.isSuccessful) {
                     this.dataTypes = dataTypeResponse.data.columns
                   }
                   else {
                     this.$Message.error({
                       content: dataTypeResponse.message,
                       showIcon: true
                     })
                   }

                 }))
                 .finally(() => this.loading = false)
      }
    },
    onSubmit()
    {
      const code = this.$route.params.source
      const database = this.$route.params.database
      const table = this.$route.params.table

      if (code && database && table) {
        this.submitting = true
        MetadataService.changeColumn(code, database, table, this.formState)
                       .then(response => {
                         if (response.status && response.data && response.data.isSuccessful) {
                           const columns = this.formState.columns.map(item => item.name)
                                               .join(', ') as string
                           this.$Message.success({
                             content: this.$t('source.tip.changeColumnSuccess').replace('$VALUE', columns),
                             showIcon: true
                           })

                           this.onCancel()
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
    onCancel()
    {
      this.visible = false
    }
  }
})
</script>
