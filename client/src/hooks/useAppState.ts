"use client";

import { useState, useEffect } from "react";

interface AppState {
  isFirstTimeUser: boolean;
  connectedRepo: string | null;
  connectedSlack: string | null;
  plan: "free" | "pro";
}

const defaultState: AppState = {
  isFirstTimeUser: true,
  connectedRepo: null,
  connectedSlack: null,
  plan: "free",
};

export function useAppState() {
  const [state, setState] = useState<AppState>(defaultState);
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem("appState");
    if (saved) {
      try {
        setState(JSON.parse(saved));
      } catch (e) {
        console.error("Failed to parse appState", e);
      }
    }
    setIsLoaded(true);
  }, []);

  const updateState = (updates: Partial<AppState>) => {
    setState((prev) => {
      const next = { ...prev, ...updates };
      localStorage.setItem("appState", JSON.stringify(next));
      return next;
    });
  };

  return { state, updateState, isLoaded };
}
