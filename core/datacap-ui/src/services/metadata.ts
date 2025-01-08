import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response'
import HttpUtils from '@/utils/http'

const DEFAULT_PATH = '/api/v1/metadata'

class MetadataService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    getDatabaseBySource(code: string): Promise<ResponseModel>
    {
        return HttpUtils.post(`${ DEFAULT_PATH }/databases/${ code }`)
    }

    getTablesByDatabase(code: string, database: string): Promise<ResponseModel>
    {
        return HttpUtils.post(`${ DEFAULT_PATH }/${ code }/tables/${ database }`)
    }
}

export default new MetadataService()