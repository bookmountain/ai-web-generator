/**
 * Code generation type enum
 */
export enum CodeGenTypeEnum {
  HTML = 'html',
  MULTI_FILE = 'multi_file',
}

/**
 * Code generation type configuration
 */
export const CODE_GEN_TYPE_CONFIG = {
  [CodeGenTypeEnum.HTML]: {
    label: 'Native HTML mode',
    value: CodeGenTypeEnum.HTML,
  },
  [CodeGenTypeEnum.MULTI_FILE]: {
    label: 'Native multi-file mode',
    value: CodeGenTypeEnum.MULTI_FILE,
  },
} as const

/**
 * Code generation type options (for dropdown selection)
 */
export const CODE_GEN_TYPE_OPTIONS = Object.values(CODE_GEN_TYPE_CONFIG).map((config) => ({
  label: config.label,
  value: config.value,
}))

/**
 * Format code generation type
 * @param type Code generation type
 * @returns Formatted type description
 */
export const formatCodeGenType = (type: string | undefined): string => {
  if (!type) return 'Unknown type'

  const config = CODE_GEN_TYPE_CONFIG[type as CodeGenTypeEnum]
  return config ? config.label : type
}

/**
 * Get all code generation types
 */
export const getAllCodeGenTypes = () => {
  return Object.values(CodeGenTypeEnum)
}

/**
 * Check whether the type is a valid code generation type
 * @param type Type to check
 */
export const isValidCodeGenType = (type: string): type is CodeGenTypeEnum => {
  return Object.values(CodeGenTypeEnum).includes(type as CodeGenTypeEnum)
}
