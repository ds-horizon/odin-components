{{/*
Common labels
*/}}
{{- define "service-deployment.selectorLabels" -}}
app.kubernetes.io/name: {{ .Release.Name }}
{{- end }}

{{- define "service-deployment.labels" -}}
com.dreamsports/service: {{ .Values.name | quote }}
{{- range $key, $value := .Values.labels }}
com.dreamsports/{{ $key }}: {{ $value | quote }}
{{- end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "service-deployment.resourceAnnotations" -}}
{{- range $key, $value := .Values.annotations }}
{{ $key }}: {{ $value | quote }}
{{- end }}
{{- end }}
