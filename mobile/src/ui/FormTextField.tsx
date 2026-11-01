import { Controller, type Control, type FieldValues, type Path } from 'react-hook-form';
import { TextField, type TextFieldProps } from './TextField';

type Props<T extends FieldValues> = Omit<TextFieldProps, 'error' | 'value' | 'onChangeText' | 'onBlur'> & {
  control: Control<T>;
  name: Path<T>;
};

export function FormTextField<T extends FieldValues>({ control, name, ...fieldProps }: Props<T>) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => (
        <TextField
          {...fieldProps}
          value={field.value ?? ''}
          onChangeText={field.onChange}
          onBlur={field.onBlur}
          error={fieldState.error?.message}
        />
      )}
    />
  );
}
