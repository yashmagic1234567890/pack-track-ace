
-- Roles
CREATE TYPE public.app_role AS ENUM ('central_admin', 'hub_user');

CREATE TABLE public.profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  full_name TEXT,
  email TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.profiles TO authenticated;
GRANT ALL ON public.profiles TO service_role;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE TABLE public.user_roles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  role public.app_role NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, role)
);
GRANT SELECT ON public.user_roles TO authenticated;
GRANT ALL ON public.user_roles TO service_role;
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION public.has_role(_user_id UUID, _role public.app_role)
RETURNS BOOLEAN
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.user_roles
    WHERE user_id = _user_id AND role = _role
  )
$$;

CREATE OR REPLACE FUNCTION public.is_central_admin(_user_id UUID)
RETURNS BOOLEAN
LANGUAGE SQL STABLE SECURITY DEFINER SET search_path = public
AS $$ SELECT public.has_role(_user_id, 'central_admin') $$;

-- Warehouses
CREATE TABLE public.warehouses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  code TEXT NOT NULL UNIQUE,
  location TEXT,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.warehouses TO authenticated;
GRANT ALL ON public.warehouses TO service_role;
ALTER TABLE public.warehouses ENABLE ROW LEVEL SECURITY;

-- User-warehouse mapping
CREATE TABLE public.user_warehouses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  warehouse_id UUID NOT NULL REFERENCES public.warehouses(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, warehouse_id)
);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_warehouses TO authenticated;
GRANT ALL ON public.user_warehouses TO service_role;
ALTER TABLE public.user_warehouses ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION public.can_access_warehouse(_user_id UUID, _warehouse_id UUID)
RETURNS BOOLEAN
LANGUAGE SQL STABLE SECURITY DEFINER SET search_path = public
AS $$
  SELECT public.has_role(_user_id, 'central_admin')
    OR EXISTS (
      SELECT 1 FROM public.user_warehouses
      WHERE user_id = _user_id AND warehouse_id = _warehouse_id
    )
$$;

-- Invoices
CREATE TABLE public.invoices (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  warehouse_id UUID NOT NULL REFERENCES public.warehouses(id) ON DELETE CASCADE,
  invoice_number TEXT NOT NULL,
  supplier TEXT,
  status TEXT NOT NULL DEFAULT 'pending',
  uploaded_by UUID REFERENCES auth.users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.invoices TO authenticated;
GRANT ALL ON public.invoices TO service_role;
ALTER TABLE public.invoices ENABLE ROW LEVEL SECURITY;

-- Invoice lines
CREATE TABLE public.invoice_lines (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  invoice_id UUID NOT NULL REFERENCES public.invoices(id) ON DELETE CASCADE,
  sku TEXT NOT NULL,
  barcode TEXT,
  product_name TEXT NOT NULL,
  unit TEXT DEFAULT 'unit',
  expected_qty NUMERIC NOT NULL DEFAULT 0,
  received_qty NUMERIC NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_invoice_lines_invoice ON public.invoice_lines(invoice_id);
CREATE INDEX idx_invoice_lines_barcode ON public.invoice_lines(invoice_id, barcode);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.invoice_lines TO authenticated;
GRANT ALL ON public.invoice_lines TO service_role;
ALTER TABLE public.invoice_lines ENABLE ROW LEVEL SECURITY;

-- Audit log
CREATE TABLE public.audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  invoice_id UUID REFERENCES public.invoices(id) ON DELETE CASCADE,
  invoice_line_id UUID REFERENCES public.invoice_lines(id) ON DELETE SET NULL,
  warehouse_id UUID REFERENCES public.warehouses(id) ON DELETE SET NULL,
  user_id UUID REFERENCES auth.users(id),
  action TEXT NOT NULL,
  details TEXT,
  qty_delta NUMERIC,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_invoice ON public.audit_logs(invoice_id);
GRANT SELECT, INSERT ON public.audit_logs TO authenticated;
GRANT ALL ON public.audit_logs TO service_role;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- updated_at trigger
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = now(); RETURN NEW; END; $$
LANGUAGE plpgsql SET search_path = public;

CREATE TRIGGER trg_profiles_updated BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER trg_warehouses_updated BEFORE UPDATE ON public.warehouses FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER trg_invoices_updated BEFORE UPDATE ON public.invoices FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER trg_invoice_lines_updated BEFORE UPDATE ON public.invoice_lines FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

-- New user -> profile
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  INSERT INTO public.profiles (id, full_name, email)
  VALUES (NEW.id, NEW.raw_user_meta_data ->> 'full_name', NEW.email)
  ON CONFLICT (id) DO NOTHING;
  RETURN NEW;
END; $$;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- RLS Policies
-- profiles
CREATE POLICY "Users view own profile" ON public.profiles FOR SELECT USING (auth.uid() = id OR public.is_central_admin(auth.uid()));
CREATE POLICY "Users update own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "Insert own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id);

-- user_roles
CREATE POLICY "View own roles or admin" ON public.user_roles FOR SELECT USING (auth.uid() = user_id OR public.is_central_admin(auth.uid()));

-- warehouses
CREATE POLICY "Admins manage warehouses" ON public.warehouses FOR ALL USING (public.is_central_admin(auth.uid())) WITH CHECK (public.is_central_admin(auth.uid()));
CREATE POLICY "Users view accessible warehouses" ON public.warehouses FOR SELECT USING (public.can_access_warehouse(auth.uid(), id));

-- user_warehouses
CREATE POLICY "Admins manage mappings" ON public.user_warehouses FOR ALL USING (public.is_central_admin(auth.uid())) WITH CHECK (public.is_central_admin(auth.uid()));
CREATE POLICY "Users view own mappings" ON public.user_warehouses FOR SELECT USING (auth.uid() = user_id OR public.is_central_admin(auth.uid()));

-- invoices
CREATE POLICY "Access invoices by warehouse" ON public.invoices FOR SELECT USING (public.can_access_warehouse(auth.uid(), warehouse_id));
CREATE POLICY "Insert invoices by warehouse" ON public.invoices FOR INSERT WITH CHECK (public.can_access_warehouse(auth.uid(), warehouse_id));
CREATE POLICY "Update invoices by warehouse" ON public.invoices FOR UPDATE USING (public.can_access_warehouse(auth.uid(), warehouse_id));
CREATE POLICY "Admins delete invoices" ON public.invoices FOR DELETE USING (public.is_central_admin(auth.uid()));

-- invoice_lines
CREATE POLICY "Access lines by warehouse" ON public.invoice_lines FOR SELECT USING (
  EXISTS (SELECT 1 FROM public.invoices i WHERE i.id = invoice_id AND public.can_access_warehouse(auth.uid(), i.warehouse_id))
);
CREATE POLICY "Insert lines by warehouse" ON public.invoice_lines FOR INSERT WITH CHECK (
  EXISTS (SELECT 1 FROM public.invoices i WHERE i.id = invoice_id AND public.can_access_warehouse(auth.uid(), i.warehouse_id))
);
CREATE POLICY "Update lines by warehouse" ON public.invoice_lines FOR UPDATE USING (
  EXISTS (SELECT 1 FROM public.invoices i WHERE i.id = invoice_id AND public.can_access_warehouse(auth.uid(), i.warehouse_id))
);
CREATE POLICY "Delete lines by warehouse" ON public.invoice_lines FOR DELETE USING (
  EXISTS (SELECT 1 FROM public.invoices i WHERE i.id = invoice_id AND public.can_access_warehouse(auth.uid(), i.warehouse_id))
);

-- audit_logs
CREATE POLICY "Access audit by warehouse" ON public.audit_logs FOR SELECT USING (
  warehouse_id IS NULL OR public.can_access_warehouse(auth.uid(), warehouse_id)
);
CREATE POLICY "Insert audit by user" ON public.audit_logs FOR INSERT WITH CHECK (auth.uid() = user_id);
