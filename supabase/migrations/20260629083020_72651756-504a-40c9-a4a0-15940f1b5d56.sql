
-- Bootstrap role for current user
CREATE OR REPLACE FUNCTION public.ensure_user_setup()
RETURNS public.app_role
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  _uid UUID := auth.uid();
  _existing public.app_role;
  _admin_count INT;
BEGIN
  IF _uid IS NULL THEN RAISE EXCEPTION 'Not authenticated'; END IF;

  SELECT role INTO _existing FROM public.user_roles WHERE user_id = _uid LIMIT 1;
  IF _existing IS NOT NULL THEN RETURN _existing; END IF;

  SELECT count(*) INTO _admin_count FROM public.user_roles WHERE role = 'central_admin';
  IF _admin_count = 0 THEN
    INSERT INTO public.user_roles (user_id, role) VALUES (_uid, 'central_admin');
    RETURN 'central_admin';
  ELSE
    INSERT INTO public.user_roles (user_id, role) VALUES (_uid, 'hub_user');
    RETURN 'hub_user';
  END IF;
END; $$;

-- Atomic scan/receive
CREATE OR REPLACE FUNCTION public.scan_receive(_line_id UUID, _delta NUMERIC DEFAULT 1, _via TEXT DEFAULT 'scan')
RETURNS public.invoice_lines
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  _uid UUID := auth.uid();
  _wh UUID;
  _inv UUID;
  _line public.invoice_lines;
BEGIN
  IF _uid IS NULL THEN RAISE EXCEPTION 'Not authenticated'; END IF;

  SELECT i.warehouse_id, i.id INTO _wh, _inv
  FROM public.invoice_lines l JOIN public.invoices i ON i.id = l.invoice_id
  WHERE l.id = _line_id;

  IF _wh IS NULL THEN RAISE EXCEPTION 'Line not found'; END IF;
  IF NOT public.can_access_warehouse(_uid, _wh) THEN RAISE EXCEPTION 'Access denied'; END IF;

  UPDATE public.invoice_lines
    SET received_qty = GREATEST(0, received_qty + _delta)
    WHERE id = _line_id
    RETURNING * INTO _line;

  INSERT INTO public.audit_logs (invoice_id, invoice_line_id, warehouse_id, user_id, action, details, qty_delta)
  VALUES (_inv, _line_id, _wh, _uid,
    CASE WHEN _via = 'scan' THEN 'SCAN' ELSE 'MANUAL_ADJUST' END,
    _line.product_name || ' (' || _line.sku || ')', _delta);

  -- update invoice status
  UPDATE public.invoices i SET status = (
    CASE WHEN (SELECT count(*) FROM public.invoice_lines l WHERE l.invoice_id = i.id AND l.received_qty < l.expected_qty) = 0
      THEN 'completed' ELSE 'in_progress' END)
  WHERE i.id = _inv;

  RETURN _line;
END; $$;

-- Admin: assign role
CREATE OR REPLACE FUNCTION public.admin_set_role(_user_id UUID, _role public.app_role)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_central_admin(auth.uid()) THEN RAISE EXCEPTION 'Access denied'; END IF;
  DELETE FROM public.user_roles WHERE user_id = _user_id;
  INSERT INTO public.user_roles (user_id, role) VALUES (_user_id, _role);
END; $$;

-- Admin: assign warehouse
CREATE OR REPLACE FUNCTION public.admin_assign_warehouse(_user_id UUID, _warehouse_id UUID)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_central_admin(auth.uid()) THEN RAISE EXCEPTION 'Access denied'; END IF;
  INSERT INTO public.user_warehouses (user_id, warehouse_id) VALUES (_user_id, _warehouse_id)
  ON CONFLICT (user_id, warehouse_id) DO NOTHING;
END; $$;

-- Admin: remove warehouse
CREATE OR REPLACE FUNCTION public.admin_remove_warehouse(_user_id UUID, _warehouse_id UUID)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_central_admin(auth.uid()) THEN RAISE EXCEPTION 'Access denied'; END IF;
  DELETE FROM public.user_warehouses WHERE user_id = _user_id AND warehouse_id = _warehouse_id;
END; $$;
