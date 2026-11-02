import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"

type DuplicateDialogProps = {
  open: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function DuplicateDialog({
  open,
  onConfirm,
  onCancel,
}: DuplicateDialogProps) {
  return (
    <AlertDialog open={open} onOpenChange={(next) => !next && onCancel()}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Possível duplicado</AlertDialogTitle>
          <AlertDialogDescription>
            Já existe um recebível parecido (mesmo cedente, valor de face e
            vencimento). Cadastrar mesmo assim?
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction onClick={onConfirm}>
            Cadastrar mesmo assim
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
