export function PageHeader({
  title,
  description,
}: {
  title: string
  description: string
}) {
  return (
    <div className="flex flex-col gap-1">
      <h2 className="font-heading text-xl font-semibold tracking-tight">
        {title}
      </h2>
      <p className="text-muted-foreground">{description}</p>
    </div>
  )
}
