import { useLayoutEffect, useRef } from "react"

import { Input } from "@/components/ui/input"
import { maskCentsInput } from "@/lib/money"

type MoneyInputProps = Omit<
  React.ComponentProps<"input">,
  "value" | "onChange" | "onPaste" | "type"
> & {
  value: string
  onValueChange: (value: string) => void
}

function isDigit(char: string) {
  return char >= "0" && char <= "9"
}

function countDigits(text: string): number {
  let count = 0
  for (const char of text) {
    if (isDigit(char)) {
      count++
    }
  }
  return count
}

function caretBeforeTrailingDigits(text: string, count: number): number {
  if (count === 0) {
    return text.length
  }
  let seen = 0
  for (let index = text.length - 1; index >= 0; index--) {
    if (isDigit(text[index]) && ++seen === count) {
      return index
    }
  }
  return 0
}

function removeDigit(text: string, from: number, step: -1 | 1) {
  for (
    let index = step === -1 ? from - 1 : from;
    index >= 0 && index < text.length;
    index += step
  ) {
    if (isDigit(text[index])) {
      return {
        text: text.slice(0, index) + text.slice(index + 1),
        caret: index,
      }
    }
  }
  return { text, caret: from }
}

export function MoneyInput({
  value,
  onValueChange,
  ...props
}: MoneyInputProps) {
  const ref = useRef<HTMLInputElement>(null)
  const pendingCaret = useRef<number | null>(null)

  useLayoutEffect(() => {
    const input = ref.current
    if (
      input &&
      pendingCaret.current !== null &&
      input === document.activeElement
    ) {
      input.setSelectionRange(pendingCaret.current, pendingCaret.current)
    }
    pendingCaret.current = null
  }, [value])

  function apply(input: HTMLInputElement, masked: string, caret: number) {
    if (masked === value) {
      input.value = masked
      input.setSelectionRange(caret, caret)
      return
    }
    pendingCaret.current = caret
    onValueChange(masked)
  }

  function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    const input = event.target
    const { inputType } = event.nativeEvent as InputEvent
    let raw = input.value
    let caret = input.selectionStart ?? raw.length

    const removedOnlySeparator =
      countDigits(raw) === countDigits(value) && raw.length < value.length
    if (removedOnlySeparator && inputType === "deleteContentBackward") {
      ;({ text: raw, caret } = removeDigit(raw, caret, -1))
    } else if (removedOnlySeparator && inputType === "deleteContentForward") {
      ;({ text: raw, caret } = removeDigit(raw, caret, 1))
    }

    const masked = maskCentsInput(raw)
    const trailingDigits = Math.min(
      countDigits(raw.slice(caret)),
      countDigits(masked)
    )
    apply(input, masked, caretBeforeTrailingDigits(masked, trailingDigits))
  }

  function handlePaste(event: React.ClipboardEvent<HTMLInputElement>) {
    event.preventDefault()
    const masked = maskCentsInput(event.clipboardData.getData("text"), true)
    apply(event.currentTarget, masked, masked.length)
  }

  return (
    <Input
      {...props}
      ref={ref}
      type="text"
      inputMode="numeric"
      autoComplete="off"
      value={value}
      onChange={handleChange}
      onPaste={handlePaste}
    />
  )
}
