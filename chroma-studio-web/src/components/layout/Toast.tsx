'use client';

import React from 'react';
import { useStudio } from '@/context/StudioContext';
import { CheckCircle } from '@phosphor-icons/react';

export default function Toast() {
  const { toast } = useStudio();

  if (!toast.visible) return null;

  return (
    <div className="fixed bottom-6 lg:bottom-10 left-1/2 -translate-x-1/2 glass-panel px-6 py-3 rounded-full font-semibold text-sm transition-all duration-400 ease-[cubic-bezier(0.175,0.885,0.32,1.275)] z-[9999] opacity-100 flex items-center gap-2 border-[var(--success)] text-[var(--success)] shadow-xl">
      <CheckCircle size={20} weight="fill" />
      <span>{toast.message}</span>
    </div>
  );
}
