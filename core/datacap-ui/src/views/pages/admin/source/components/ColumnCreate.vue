<template>
  <ShadcnModal v-model="visible"
               height="80%"
               width="40%"
               :title="$t('source.common.newColumn')"
               @on-close="onCancel">
    <ShadcnForm v-model="formState" v-if="formState" @on-submit="onSubmit">
      <ShadcnRow gutter="16">
        <ShadcnButton circle size="small" @click="onAdd">
          <ShadcnIcon icon="Plus" size="15"/>
        </ShadcnButton>

        <ShadcnCol v-for="(item, index) in formState.columns" span="12">
          <ShadcnDivider orientation="left">{{ item.name }}</ShadcnDivider>

          <ShadcnButton circle
                        size="small"
                        type="error"
                        :disabled="item.removed"
                        @click="onRemove(index)">
            <ShadcnIcon icon="Minus" size="15"/>
          </ShadcnButton>

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
                <ShadcnSwitch v-model="formState.columns[index].isNullable" :placeholder="$t('source.placeholder.columnIsNullable')" :name="`columns[${index}].isNullable`"/>
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

export default defineComponent({
  name: 'ColumnCreate',
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
      saving: false,
      dataTypes: [],
      formState: null as any
    }
  },
  created()
  {
    const code = this.$route.params.source

    this.formState = {
      columns: []
    }

    this.loading = true
    MetadataService.getDataTypes(code)
                   .then(response => {
                     if (response.status && response.data && response.data.isSuccessful) {
                       this.dataTypes = response.data.columns
                     }
                   })
                   .finally(() => this.loading = false)

    this.onAdd()
  },
  methods: {
    onSubmit()
    {
      const code = this.$route.params.source
      const database = this.$route.params.database
      const table = this.$route.params.table

      if (code && database && table) {
        this.saving = true
        MetadataService.createColumn(code, database, table, this.formState)
                       .then(response => {
                         if (response.status && response.data && response.data.isSuccessful) {
                           const columns = String(this.formState?.columns?.map(item => item.name).join(', '))
                           this.$Message.success({
                             content: this.$t('source.tip.createColumnSuccess').replace('$VALUE', columns),
                             showIcon: true
                           })

                           this.onCancel()
                         }
                         else {
                           this.$Message.error({
                             content: response.data.message,
                             showIcon: true
                           })
                         }
                       })
                       .finally(() => this.saving = false)
      }
    },
    onAdd()
    {
      const newColumn = {
        removed: false
      }
      if (this.formState.columns.length === 0) {
        newColumn.removed = true
      }
      this.formState.columns.push(newColumn)
    },
    onRemove(index: number)
    {
      if (this.formState.columns) {
        this.formState.columns.splice(index, 1)
      }
    },
    onCancel()
    {
      this.visible = false
    }
  }
})
</script>
