import { Component, computed, input } from '@angular/core';

interface EtapeAffichage {
  label: string;
  numero: number;
  fait: boolean;
  active: boolean;
  aLigne: boolean;
}

/** En-tête de formulaire par étapes — cercles numérotés reliés par une ligne ;
 * étape complétée = ✓ + ligne accent. À réutiliser pour toute création multi-champs. */
@Component({
  selector: 'app-stepper',
  template: `
    <div class="stepper">
      @for (etape of etapes(); track etape.numero) {
        <div class="etape">
          <div class="etape-marque">
            <div class="etape-cercle" [class.fait]="etape.fait" [class.active]="etape.active">
              {{ etape.fait ? '✓' : etape.numero }}
            </div>
            <span class="etape-label" [class.fait]="etape.fait" [class.active]="etape.active">{{ etape.label }}</span>
          </div>
          @if (etape.aLigne) {
            <div class="etape-ligne" [class.fait]="etape.fait"></div>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .stepper {
        display: flex;
        align-items: center;
        margin-bottom: 28px;
      }

      .etape {
        display: flex;
        align-items: center;
        flex: 1;
      }

      .etape-marque {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 6px;
        flex-shrink: 0;
      }

      .etape-cercle {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 13px;
        font-weight: 700;
        background: var(--surface-2);
        color: var(--text-muted);
        border: 2px solid var(--border-strong);
      }

      .etape-cercle.fait,
      .etape-cercle.active {
        background: var(--accent);
        color: var(--text-on-accent);
        border-color: var(--accent);
      }

      .etape-label {
        font-size: 12px;
        font-weight: 600;
        color: var(--text-muted);
        white-space: nowrap;
      }

      .etape-label.fait {
        color: var(--text);
      }

      .etape-label.active {
        color: var(--accent);
      }

      .etape-ligne {
        height: 2px;
        flex: 1;
        background: var(--border-strong);
        margin: 0 10px 20px;
      }

      .etape-ligne.fait {
        background: var(--accent);
      }
    `
  ]
})
export class Stepper {
  readonly labels = input.required<string[]>();
  readonly currentStep = input.required<number>();

  protected readonly etapes = computed<EtapeAffichage[]>(() => {
    const cur = this.currentStep();
    const total = this.labels().length;
    return this.labels().map((label, i) => {
      const numero = i + 1;
      return {
        label,
        numero,
        fait: numero < cur,
        active: numero === cur,
        aLigne: numero < total
      };
    });
  });
}
