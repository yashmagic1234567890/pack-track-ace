import { useEffect, useState, useCallback } from "react";
import { supabase } from "@/integrations/supabase/client";

export type AppRole = "central_admin" | "hub_user";

export interface Warehouse {
  id: string;
  name: string;
  code: string;
  location: string | null;
  is_active: boolean;
}

export interface Invoice {
  id: string;
  warehouse_id: string;
  invoice_number: string;
  supplier: string | null;
  status: string;
  created_at: string;
}

export interface InvoiceLine {
  id: string;
  invoice_id: string;
  sku: string;
  barcode: string | null;
  product_name: string;
  unit: string | null;
  expected_qty: number;
  received_qty: number;
}

export interface AuthState {
  loading: boolean;
  userId: string | null;
  email: string | null;
  role: AppRole | null;
  warehouses: Warehouse[];
}

/** Central auth + role hook. Bootstraps the user's role on first login. */
export function useAuth() {
  const [state, setState] = useState<AuthState>({
    loading: true,
    userId: null,
    email: null,
    role: null,
    warehouses: [],
  });

  const load = useCallback(async () => {
    const { data: sessionData } = await supabase.auth.getSession();
    const session = sessionData.session;
    if (!session) {
      setState({ loading: false, userId: null, email: null, role: null, warehouses: [] });
      return;
    }
    // Ensure role exists (first user becomes central_admin)
    const { data: role } = await supabase.rpc("ensure_user_setup");

    let warehouses: Warehouse[] = [];
    if (role === "central_admin") {
      const { data } = await supabase.from("warehouses").select("*").order("name");
      warehouses = (data as Warehouse[]) ?? [];
    } else {
      const { data } = await supabase
        .from("user_warehouses")
        .select("warehouses(*)")
        .eq("user_id", session.user.id);
      warehouses = ((data as { warehouses: Warehouse }[]) ?? [])
        .map((r) => r.warehouses)
        .filter(Boolean);
    }

    setState({
      loading: false,
      userId: session.user.id,
      email: session.user.email ?? null,
      role: (role as AppRole) ?? null,
      warehouses,
    });
  }, []);

  useEffect(() => {
    const { data: sub } = supabase.auth.onAuthStateChange(() => {
      void load();
    });
    void load();
    return () => sub.subscription.unsubscribe();
  }, [load]);

  return { ...state, reload: load };
}

export async function signOut() {
  await supabase.auth.signOut();
}
