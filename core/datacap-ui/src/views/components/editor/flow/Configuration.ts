export interface Configuration
{
    id?: number
    name?: string
    type?: string
    protocol?: string
    nodeType?: string
    configure?: any
    elements?: Array<any>
    transform?: any
    fitViewOnInit?: boolean
}

export class ConfigurationRequest
{
    public static of(): Configuration
    {
        return {
            elements: []
        }
    }
}
