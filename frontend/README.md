# Luis Amigo - Frontend

Frontend web para Luis Amigo, el asistente jurídico académico con IA para estudiantes de Derecho UACH.

## Tecnologías

- **Next.js 14** - Framework React con App Router
- **TypeScript** - Tipado estático
- **Tailwind CSS** - Estilos utility-first
- **Axios** - Cliente HTTP
- **React Markdown** - Renderizado de markdown

## Desarrollo

1. Instalar dependencias:
```bash
npm install
```

2. Configurar variables de entorno (`.env.local`):
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

3. Iniciar servidor de desarrollo:
```bash
npm run dev
```

4. Abrir [http://localhost:3000](http://localhost:3000)

## Features

- ✅ Chat interface limpia y responsive
- ✅ Integración completa con backend RAG
- ✅ Visualización de fuentes citadas con scores de similitud
- ✅ Filtros por materia y semestre
- ✅ Markdown rendering para respuestas formateadas
- ✅ Loading states y error handling
- ✅ Mobile-friendly

## Build para Producción

```bash
npm run build
npm start
```

## Estructura del Proyecto

```
frontend/
├── app/
│   ├── layout.tsx      # Layout principal
│   ├── page.tsx        # Página de chat
│   └── globals.css     # Estilos globales
├── components/         # Componentes reutilizables
├── lib/
│   └── api.ts          # Cliente API
├── types/
│   └── index.ts        # Tipos TypeScript
└── public/             # Assets estáticos
```

## License

MIT
